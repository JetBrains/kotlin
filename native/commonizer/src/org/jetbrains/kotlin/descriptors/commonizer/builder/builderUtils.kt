/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.builder

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.commonizer.cir.*
import org.jetbrains.kotlin.descriptors.commonizer.utils.isUnderStandardKotlinPackages
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
    val ownTypeParameters = mutableListOf<TypeParameterDescriptor>()

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
): SimpleType {
    val classifier: ClassifierDescriptor = when (val classifierId = classifierId) {
        is CirClassifierId.Class -> {
            targetComponents.findClassOrTypeAlias(classifierId.classId).checkClassifierType<ClassDescriptor>()
        }
        is CirClassifierId.TypeAlias -> {
            val classId = classifierId.classId
            val classOrTypeAlias: ClassifierDescriptorWithTypeParameters = targetComponents.findClassOrTypeAlias(classId)

            if (classId.packageFqName.isUnderStandardKotlinPackages || !targetComponents.isCommon) {
                // classifier type could be only type alias
                classOrTypeAlias.checkClassifierType<TypeAliasDescriptor>()
            } else {
                // classifier could be class or type alias
                classOrTypeAlias
            }
        }
        is CirClassifierId.TypeParameter -> {
            typeParameterResolver.resolve(classifierId.index)
                ?: error("Type parameter $classifierId not found in ${typeParameterResolver::class.java}, $typeParameterResolver for ${targetComponents.target}")
        }
    }

    val arguments = arguments.map { it.buildArgument(targetComponents, typeParameterResolver, expandTypeAliases) }
    val simpleType = simpleType(
        annotations = Annotations.EMPTY,
        constructor = classifier.typeConstructor,
        arguments = arguments,
        nullable = isMarkedNullable,
        kotlinTypeRefiner = null
    )

    return if (expandTypeAliases && classifier is TypeAliasDescriptor) {
        val expandedType = TypeAliasExpander.NON_REPORTING.expandWithoutAbbreviation(
            TypeAliasExpansion.create(null, classifier, arguments),
            Annotations.EMPTY
        )

        expandedType.makeNullableAsSpecified(simpleType.isMarkedNullable).withAbbreviation(simpleType)
    } else
        simpleType
}

private inline fun <reified T : ClassifierDescriptorWithTypeParameters> ClassifierDescriptorWithTypeParameters.checkClassifierType(): T {
    check(this is T) { "Mismatched classifier kinds.\nFound: ${this::class.java}, $this\nShould be: ${T::class.java}" }
    return this
}

private fun CirTypeProjection.buildArgument(
    targetComponents: TargetDeclarationsBuilderComponents,
    typeParameterResolver: TypeParameterResolver,
    expandTypeAliases: Boolean
): TypeProjection =
    if (isStarProjection) {
        StarProjectionForAbsentTypeParameter(targetComponents.builtIns)
    } else {
        TypeProjectionImpl(projectionKind, type.buildType(targetComponents, typeParameterResolver, expandTypeAliases))
    }
