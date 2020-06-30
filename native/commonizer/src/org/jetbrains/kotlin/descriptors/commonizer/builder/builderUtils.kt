/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.builder

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.commonizer.cir.*
import org.jetbrains.kotlin.descriptors.commonizer.utils.isUnderStandardKotlinPackages
import org.jetbrains.kotlin.descriptors.commonizer.utils.resolveClassOrTypeAlias
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.resolve.DescriptorFactory
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.KotlinTypeFactory.flexibleType
import org.jetbrains.kotlin.types.KotlinTypeFactory.simpleType

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
            annotations = param.annotations.buildDescriptors(targetComponents),
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
        param.annotations.buildDescriptors(targetComponents),
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
    val classifier: ClassifierDescriptor = when (val classifierId = classifierId) {
        is CirClassifierId.Class -> {
            findClassOrTypeAlias(targetComponents, classifierId.classId).checkClassifierType<ClassDescriptor>()
        }
        is CirClassifierId.TypeAlias -> {
            val classId = classifierId.classId
            val classOrTypeAlias: ClassifierDescriptorWithTypeParameters = findClassOrTypeAlias(targetComponents, classId)

            if (classId.packageFqName.isUnderStandardKotlinPackages || !targetComponents.isCommon) {
                // classifier type could be only type alias
                classOrTypeAlias.checkClassifierType<TypeAliasDescriptor>()
            } else {
                // classifier could be class or type alias
                classOrTypeAlias
            }
        }
        is CirClassifierId.TypeParameter -> {
            val name = classifierId.name
            typeParameterResolver.resolve(name)
                ?: error("Type parameter $name not found in ${typeParameterResolver::class.java}, $typeParameterResolver for ${targetComponents.target}")
        }
    }

    val simpleType = simpleType(
        annotations = Annotations.EMPTY,
        constructor = classifier.typeConstructor,
        arguments = arguments.map { it.buildArgument(targetComponents, typeParameterResolver) },
        nullable = isMarkedNullable,
        kotlinTypeRefiner = null
    )

    val computedType = if (classifier is TypeAliasDescriptor)
        classifier.underlyingType.withAbbreviation(simpleType)
    else
        simpleType

    return if (isDefinitelyNotNullType)
        computedType.makeSimpleTypeDefinitelyNotNullOrNotNull()
    else
        computedType
}

internal fun findClassOrTypeAlias(
    targetComponents: TargetDeclarationsBuilderComponents,
    classId: ClassId
): ClassifierDescriptorWithTypeParameters = when {
    classId.packageFqName.isUnderStandardKotlinPackages -> {
        // look up for classifier in built-ins module:
        val builtInsModule = targetComponents.builtIns.builtInsModule

        // TODO: this works fine for Native as far as built-ins module contains full Native stdlib, but this is not enough for JVM and JS
        builtInsModule.resolveClassOrTypeAlias(classId)
            ?: error("Classifier ${classId.asString()} not found in built-ins module $builtInsModule for ${targetComponents.target}")
    }

    else -> {
        // otherwise, find the appropriate user classifier:
        targetComponents.findAppropriateClassOrTypeAlias(classId)
            ?: error("Classifier ${classId.asString()} not found in created descriptors cache for ${targetComponents.target}")
    }
}

private inline fun <reified T : ClassifierDescriptorWithTypeParameters> ClassifierDescriptorWithTypeParameters.checkClassifierType(): T {
    check(this is T) { "Mismatched classifier kinds.\nFound: ${this::class.java}, $this\nShould be: ${T::class.java}" }
    return this
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
