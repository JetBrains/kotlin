/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.builder

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.commonizer.cir.*
import org.jetbrains.kotlin.descriptors.commonizer.utils.compact
import org.jetbrains.kotlin.descriptors.commonizer.utils.compactMap
import org.jetbrains.kotlin.descriptors.commonizer.utils.compactMapIndexed
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
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
    val ownTypeParameters = ArrayList<TypeParameterDescriptor>(size)

    val typeParameterResolver = TypeParameterResolverImpl(
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
): List<ValueParameterDescriptor> = compactMapIndexed { index, param ->
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
    Annotations.create(compactMap { CommonizedAnnotationDescriptor(targetComponents, it) })

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
    typeParameterResolver: TypeParameterResolver,
    expandTypeAliases: Boolean = true
): UnwrappedType = when (this) {
    is CirSimpleType -> buildType(targetComponents, typeParameterResolver, expandTypeAliases)
    is CirFlexibleType -> flexibleType(
        lowerBound = lowerBound.buildType(targetComponents, typeParameterResolver, expandTypeAliases),
        upperBound = upperBound.buildType(targetComponents, typeParameterResolver, expandTypeAliases)
    )
}

internal fun CirSimpleType.buildType(
    targetComponents: TargetDeclarationsBuilderComponents,
    typeParameterResolver: TypeParameterResolver,
    expandTypeAliases: Boolean
): SimpleType = when (this) {
    is CirClassType -> buildSimpleType(
        classifier = targetComponents.findClassOrTypeAlias(classifierId).checkClassifierType<ClassDescriptor>(),
        arguments = collectArguments(targetComponents, typeParameterResolver, expandTypeAliases),
        isMarkedNullable = isMarkedNullable
    )
    is CirTypeAliasType -> {
        val typeAliasDescriptor = targetComponents.findClassOrTypeAlias(classifierId).checkClassifierType<TypeAliasDescriptor>()
        val arguments = this.arguments.compactMap { it.buildArgument(targetComponents, typeParameterResolver, expandTypeAliases) }

        if (expandTypeAliases)
            buildExpandedType(typeAliasDescriptor, arguments, isMarkedNullable)
        else
            buildSimpleType(typeAliasDescriptor, arguments, isMarkedNullable)
    }
    is CirTypeParameterType -> buildSimpleType(
        classifier = typeParameterResolver.resolve(index)
            ?: error("Type parameter with index=$index not found in ${typeParameterResolver::class.java}, $typeParameterResolver for ${targetComponents.target}"),
        arguments = emptyList(),
        isMarkedNullable = isMarkedNullable
    )
}

@Suppress("NOTHING_TO_INLINE")
private inline fun buildSimpleType(classifier: ClassifierDescriptor, arguments: List<TypeProjection>, isMarkedNullable: Boolean) =
    simpleType(
        annotations = Annotations.EMPTY,
        constructor = classifier.typeConstructor,
        arguments = arguments,
        nullable = isMarkedNullable,
        kotlinTypeRefiner = null
    )

@Suppress("NOTHING_TO_INLINE")
private inline fun buildExpandedType(classifier: TypeAliasDescriptor, arguments: List<TypeProjection>, isMarkedNullable: Boolean) =
    TypeAliasExpander.NON_REPORTING.expand(
        TypeAliasExpansion.create(null, classifier, arguments),
        Annotations.EMPTY
    ).makeNullableAsSpecified(isMarkedNullable)


private inline fun <reified T : ClassifierDescriptorWithTypeParameters> ClassifierDescriptorWithTypeParameters.checkClassifierType(): T {
    check(this is T) { "Mismatched classifier kinds.\nFound: ${this::class.java}, $this\nShould be: ${T::class.java}" }
    return this
}

private fun CirClassType.collectArguments(
    targetComponents: TargetDeclarationsBuilderComponents,
    typeParameterResolver: TypeParameterResolver,
    expandTypeAliases: Boolean
): List<TypeProjection> {
    return if (outerType == null) {
        arguments.compactMap { it.buildArgument(targetComponents, typeParameterResolver, expandTypeAliases) }
    } else {
        val allTypes = generateSequence(this) { it.outerType }.toList()
        val arguments = mutableListOf<TypeProjection>()

        for (index in allTypes.size - 1 downTo 0) {
            allTypes[index].arguments.mapTo(arguments) { it.buildArgument(targetComponents, typeParameterResolver, expandTypeAliases) }
        }

        arguments.compact()
    }
}

private fun CirTypeProjection.buildArgument(
    targetComponents: TargetDeclarationsBuilderComponents,
    typeParameterResolver: TypeParameterResolver,
    expandTypeAliases: Boolean
): TypeProjection = when (this) {
    is CirStarTypeProjection -> StarProjectionForAbsentTypeParameter(targetComponents.builtIns)
    is CirTypeProjectionImpl -> TypeProjectionImpl(
        projectionKind,
        type.buildType(targetComponents, typeParameterResolver, expandTypeAliases)
    )
}
