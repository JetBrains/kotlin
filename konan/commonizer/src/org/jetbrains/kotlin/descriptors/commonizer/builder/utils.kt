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
import org.jetbrains.kotlin.name.FqName
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

internal fun List<CirAnnotation>.buildDescriptors(targetComponents: TargetDeclarationsBuilderComponents): Annotations =
    if (isEmpty())
        Annotations.EMPTY
    else
        Annotations.create(map { CommonizedAnnotationDescriptor(targetComponents, it) })

internal fun CirExtensionReceiver.buildExtensionReceiver(
    targetComponents: TargetDeclarationsBuilderComponents,
    typeParameterResolver: TypeParameterResolver,
    containingDeclaration: CallableDescriptor
) = DescriptorFactory.createExtensionReceiverParameterForCallable(
    containingDeclaration,
    type.buildType(targetComponents, typeParameterResolver),
    annotations.buildDescriptors(targetComponents)
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
            val classOrTypeAlias = findClassOrTypeAlias(targetComponents, fqName)
            checkClassifier(classOrTypeAlias, kind, fqName.isUnderStandardKotlinPackages || !targetComponents.isCommon)
            classOrTypeAlias
        }
    }

    // TODO: commonize annotations, KT-34234
    val typeAnnotations = if (!targetComponents.isCommon) annotations.buildDescriptors(targetComponents) else Annotations.EMPTY
    val rawType = simpleType(
        annotations = typeAnnotations,
        constructor = classifier.typeConstructor,
        arguments = arguments.map { it.buildArgument(targetComponents, typeParameterResolver) },
        nullable = isMarkedNullable,
        kotlinTypeRefiner = null
    )

    return if (isDefinitelyNotNullType) rawType.makeSimpleTypeDefinitelyNotNullOrNotNull() else rawType
}

internal fun findClassOrTypeAlias(
    targetComponents: TargetDeclarationsBuilderComponents,
    fqName: FqName
): ClassifierDescriptorWithTypeParameters {
    return if (fqName.isUnderStandardKotlinPackages) {
        // look up for classifier in built-ins module:
        val builtInsModule = targetComponents.builtIns.builtInsModule
        // TODO: this works fine for Native as far as built-ins module contains full Native stdlib, but this is not enough for JVM and JS
        builtInsModule.getPackage(fqName.parent())
            .memberScope
            .getContributedClassifier(fqName.shortName(), NoLookupLocation.FOR_ALREADY_TRACKED)
            ?.cast()
            ?: error("Classifier $fqName not found in built-ins module $builtInsModule")
    } else {
        // otherwise, look up in created descriptors cache:
        targetComponents.getCachedClassifier(fqName)
            ?: error("Classifier $fqName not found in created descriptors cache")
    }
}

private fun checkClassifier(classifier: ClassifierDescriptor, kind: CirSimpleTypeKind, strict: Boolean) {
    val classifierKind = CirSimpleTypeKind.determineKind(classifier)

    if (strict) {
        check(kind == classifierKind) {
            "Mismatched classifier kinds.\nFound: $classifierKind, ${classifier::class.java}, $classifier\nShould be: $kind"
        }
    } else {
        check(areCompatible(classifierKind, kind)) {
            "Incompatible classifier kinds.\nExpect: $classifierKind, ${classifier::class.java}, $classifier\nActual: $kind"
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
