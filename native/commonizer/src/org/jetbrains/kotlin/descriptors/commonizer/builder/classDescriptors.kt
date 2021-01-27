/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.builder

import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.ClassifierDescriptorWithTypeParameters
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirClass
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirClassConstructor
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.CirClassConstructorNode
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.CirClassNode
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.CirNode.Companion.indexOfCommon
import org.jetbrains.kotlin.descriptors.commonizer.utils.CommonizedGroup
import org.jetbrains.kotlin.name.ClassId

internal fun CirClassNode.buildDescriptors(
    components: GlobalDeclarationsBuilderComponents,
    output: CommonizedGroup<ClassifierDescriptorWithTypeParameters>,
    containingDeclarations: List<DeclarationDescriptor?>
) {
    val commonClass = commonDeclaration()
    val markAsActual = commonClass != null && commonClass.kind != ClassKind.ENUM_ENTRY

    targetDeclarations.forEachIndexed { index, clazz ->
        clazz?.buildDescriptor(components, output, index, containingDeclarations, classId, isActual = markAsActual)
    }

    commonClass?.buildDescriptor(components, output, indexOfCommon, containingDeclarations, classId, isExpect = true)
}

internal fun CirClass.buildDescriptor(
    components: GlobalDeclarationsBuilderComponents,
    output: CommonizedGroup<in ClassifierDescriptorWithTypeParameters>,
    index: Int,
    containingDeclarations: List<DeclarationDescriptor?>,
    classId: ClassId,
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
        companionObjectName = companion,
        cirSupertypes = supertypes
    )

    // cache created class descriptor:
    components.cache.cache(classId, index, classDescriptor)

    output[index] = classDescriptor
}

internal fun CirClassConstructorNode.buildDescriptors(
    components: GlobalDeclarationsBuilderComponents,
    output: CommonizedGroup<ClassConstructorDescriptor>,
    containingDeclarations: List<CommonizedClassDescriptor?>
) {
    val commonConstructor = commonDeclaration()
    val markAsActual = commonConstructor != null

    targetDeclarations.forEachIndexed { index, constructor ->
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
        isPrimary = isPrimary
    )

    constructorDescriptor.isExpect = isExpect
    constructorDescriptor.isActual = isActual

    constructorDescriptor.setHasStableParameterNames(hasStableParameterNames)

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
