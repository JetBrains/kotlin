/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.builder

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.commonizer.isUnderStandardKotlinPackages
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.*
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.CirSimpleTypeKind.Companion.areCompatible
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.resolve.DescriptorFactory
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.KotlinTypeFactory.flexibleType
import org.jetbrains.kotlin.types.KotlinTypeFactory.simpleType
import org.jetbrains.kotlin.utils.addToStdlib.cast

internal fun List<CirTypeParameter>.buildDescriptorsAndTypeParameterResolver(
    targetComponents: TargetDeclarationsBuilderComponents,
    parentTypeParameterResolver: TypeParameterResolver,
    containingDeclaration: DeclarationDescriptor,
    typeParametersIndexOffset: Int = 0
): Pair<List<TypeParameterDescriptor>, TypeParameterResolver> {
    val ownTypeParameters = mutableListOf<TypeParameterDescriptor>()

    val typeParameterResolver = TypeParameterResolverImpl(
        storageManager = targetComponents.storageManager,
        ownTypeParameters = ownTypeParameters,
        parent = parentTypeParameterResolver
    )

    forEachIndexed { index, param ->
        ownTypeParameters += CommonizedTypeParameterDescriptor(
            targetComponents = targetComponents,
            typeParameterResolver = typeParameterResolver,
            containingDeclaration = containingDeclaration,
            annotations = param.annotations,
            name = param.name,
            variance = param.variance,
            isReified = param.isReified,
            index = typeParametersIndexOffset + index,
            cirUpperBounds = param.upperBounds
        )
    }

    return ownTypeParameters to typeParameterResolver
}


internal fun List<CirValueParameter>.buildDescriptors(
    targetComponents: TargetDeclarationsBuilderComponents,
    typeParameterResolver: TypeParameterResolver,
    containingDeclaration: CallableDescriptor
): List<ValueParameterDescriptor> = mapIndexed { index, param ->
    ValueParameterDescriptorImpl(
        containingDeclaration,
        null,
        index,
        param.annotations,
        param.name,
        param.returnType.buildType(targetComponents, typeParameterResolver),
        param.declaresDefaultValue,
        param.isCrossinline,
        param.isNoinline,
        param.varargElementType?.buildType(targetComponents, typeParameterResolver),
        SourceElement.NO_SOURCE
    )
}

internal fun CirExtensionReceiver.buildExtensionReceiver(
    targetComponents: TargetDeclarationsBuilderComponents,
    typeParameterResolver: TypeParameterResolver,
    containingDeclaration: CallableDescriptor
) = DescriptorFactory.createExtensionReceiverParameterForCallable(
    containingDeclaration,
    type.buildType(targetComponents, typeParameterResolver),
    annotations
)

internal fun buildDispatchReceiver(callableDescriptor: CallableDescriptor) =
    DescriptorUtils.getDispatchReceiverParameterIfNeeded(callableDescriptor.containingDeclaration)

internal fun CirType.buildType(
    targetComponents: TargetDeclarationsBuilderComponents,
    typeParameterResolver: TypeParameterResolver
): UnwrappedType = when (this) {
    is CirSimpleType -> buildType(targetComponents, typeParameterResolver)
    is CirFlexibleType -> flexibleType(
        lowerBound = lowerBound.buildType(targetComponents, typeParameterResolver),
        upperBound = upperBound.buildType(targetComponents, typeParameterResolver)
    )
}

internal fun CirSimpleType.buildType(
    targetComponents: TargetDeclarationsBuilderComponents,
    typeParameterResolver: TypeParameterResolver
): SimpleType {
    val classifier: ClassifierDescriptor = when (kind) {

        CirSimpleTypeKind.TYPE_PARAMETER -> {
            typeParameterResolver.resolve(fqName.shortName())
                ?: error("Type parameter $fqName not found in ${typeParameterResolver::class.java}, $typeParameterResolver")
        }

        CirSimpleTypeKind.CLASS, CirSimpleTypeKind.TYPE_ALIAS -> {
            if (fqName.isUnderStandardKotlinPackages) {
                // look up for classifier in built-ins module:
                val builtInsModule = targetComponents.builtIns.builtInsModule
                // TODO: this works fine for Native as far as built-ins module contains full Native stdlib, but this is not enough for JVM and JS
                builtInsModule.getPackage(fqName.parent())
                    .memberScope
                    .getContributedClassifier(fqName.shortName(), NoLookupLocation.FOR_ALREADY_TRACKED)
                    ?.cast<ClassifierDescriptorWithTypeParameters>()
                    ?.also { checkClassifier(it, kind, true) }
                    ?: error("Classifier $fqName not found in built-ins module $builtInsModule")
            } else {
                // otherwise, look up in created descriptors cache:
                targetComponents.getCachedClassifier(fqName)
                    ?.also { checkClassifier(it, kind, !targetComponents.isCommon) }
                    ?: error("Classifier $fqName not found in created descriptors cache")
            }
        }
    }

    val rawType = simpleType(
        annotations = Annotations.EMPTY, // TODO: support annotations
        constructor = classifier.typeConstructor,
        arguments = arguments.map { it.buildArgument(targetComponents, typeParameterResolver) },
        nullable = isMarkedNullable,
        kotlinTypeRefiner = null
    )

    return if (isDefinitelyNotNullType) rawType.makeSimpleTypeDefinitelyNotNullOrNotNull() else rawType
}

private fun checkClassifier(classifier: ClassifierDescriptor, kindInCir: CirSimpleTypeKind, strict: Boolean) {
    val classifierKind = CirSimpleTypeKind.determineKind(classifier)

    if (strict) {
        check(kindInCir == classifierKind) {
            "Mismatched classifier kinds.\nFound: $classifierKind, ${classifier::class.java}, $classifier\nShould be: $kindInCir"
        }
    } else {
        check(areCompatible(classifierKind, kindInCir)) {
            "Incompatible classifier kinds.\nExpect: $classifierKind, ${classifier::class.java}, $classifier\nActual: $kindInCir"
        }
    }
}

private fun CirTypeProjection.buildArgument(
    targetComponents: TargetDeclarationsBuilderComponents,
    typeParameterResolver: TypeParameterResolver
): TypeProjection =
    if (isStarProjection) {
        StarProjectionForAbsentTypeParameter(targetComponents.builtIns)
    } else {
        TypeProjectionImpl(projectionKind, type.buildType(targetComponents, typeParameterResolver))
    }
