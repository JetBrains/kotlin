/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.builder

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.commonizer.CommonizedGroup
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.*
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.indexOfCommon
import org.jetbrains.kotlin.name.FqName

internal fun CirClassNode.buildDescriptors(
    components: GlobalDeclarationsBuilderComponents,
    output: CommonizedGroup<ClassifierDescriptorWithTypeParameters>,
    containingDeclarations: List<DeclarationDescriptor?>
) {
    val commonClass = common()
    val markAsActual = commonClass != null && commonClass.kind != ClassKind.ENUM_ENTRY

    target.forEachIndexed { index, clazz ->
        clazz?.buildDescriptor(components, output, index, containingDeclarations, fqName, isActual = markAsActual)
    }

    commonClass?.buildDescriptor(components, output, indexOfCommon, containingDeclarations, fqName, isExpect = true)
}

internal fun CirClass.buildDescriptor(
    components: GlobalDeclarationsBuilderComponents,
    output: CommonizedGroup<in ClassifierDescriptorWithTypeParameters>,
    index: Int,
    containingDeclarations: List<DeclarationDescriptor?>,
    fqName: FqName,
    isExpect: Boolean = false,
    isActual: Boolean = false
) {
    val targetComponents = components.targetComponents[index]
    val containingDeclaration = containingDeclarations[index] ?: error("No containing declaration for class $this")

    val classDescriptor = CommonizedClassDescriptor(
        targetComponents = targetComponents,
        containingDeclaration = containingDeclaration,
        annotations = annotations.buildDescriptors(targetComponents),
        name = name,
        kind = kind,
        modality = modality,
        visibility = visibility,
        isCompanion = isCompanion,
        isData = isData,
        isInline = isInline,
        isInner = isInner,
        isExternal = isExternal,
        isExpect = isExpect,
        isActual = isActual,
        cirDeclaredTypeParameters = typeParameters,
        companionObjectName = companion?.shortName(),
        cirSupertypes = supertypes
    )

    // cache created class descriptor:
    components.cache.cache(fqName, index, classDescriptor)

    output[index] = classDescriptor
}

internal fun CirClassConstructorNode.buildDescriptors(
    components: GlobalDeclarationsBuilderComponents,
    output: CommonizedGroup<ClassConstructorDescriptor>,
    containingDeclarations: List<CommonizedClassDescriptor?>
) {
    val commonConstructor = common()
    val markAsActual = commonConstructor != null

    target.forEachIndexed { index, constructor ->
        constructor?.buildDescriptor(components, output, index, containingDeclarations, isActual = markAsActual)
    }

    commonConstructor?.buildDescriptor(components, output, indexOfCommon, containingDeclarations, isExpect = true)
}

private fun CirClassConstructor.buildDescriptor(
    components: GlobalDeclarationsBuilderComponents,
    output: CommonizedGroup<ClassConstructorDescriptor>,
    index: Int,
    containingDeclarations: List<CommonizedClassDescriptor?>,
    isExpect: Boolean = false,
    isActual: Boolean = false
) {
    val targetComponents = components.targetComponents[index]
    val containingDeclaration = containingDeclarations[index] ?: error("No containing declaration for constructor $this")

    val constructorDescriptor = CommonizedClassConstructorDescriptor(
        containingDeclaration = containingDeclaration,
        annotations = annotations.buildDescriptors(targetComponents),
        isPrimary = isPrimary,
        kind = kind
    )

    constructorDescriptor.isExpect = isExpect
    constructorDescriptor.isActual = isActual

    constructorDescriptor.setHasStableParameterNames(hasStableParameterNames)
    constructorDescriptor.setHasSynthesizedParameterNames(hasSynthesizedParameterNames)

    val classTypeParameters = containingDeclaration.declaredTypeParameters
    val (constructorTypeParameters, typeParameterResolver) = typeParameters.buildDescriptorsAndTypeParameterResolver(
        targetComponents = targetComponents,
        parentTypeParameterResolver = containingDeclaration.typeParameterResolver,
        containingDeclaration = constructorDescriptor,
        typeParametersIndexOffset = classTypeParameters.size
    )

    constructorDescriptor.initialize(
        valueParameters.buildDescriptors(targetComponents, typeParameterResolver, constructorDescriptor),
        visibility,
        classTypeParameters + constructorTypeParameters
    )

    output[index] = constructorDescriptor
}
