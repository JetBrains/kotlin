/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.builder

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.commonizer.CommonizedGroup
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.*
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.indexOfCommon
import org.jetbrains.kotlin.storage.StorageManager

internal fun ClassNode.buildDescriptors(
    output: CommonizedGroup<ClassifierDescriptorWithTypeParameters>,
    containingDeclarations: List<DeclarationDescriptor?>,
    storageManager: StorageManager
) {
    val commonClass = common()
    val markAsActual = commonClass != null && commonClass.kind != ClassKind.ENUM_ENTRY

    target.forEachIndexed { index, clazz ->
        clazz?.buildDescriptor(output, index, containingDeclarations, storageManager, isActual = markAsActual)
    }

    commonClass?.buildDescriptor(output, indexOfCommon, containingDeclarations, storageManager, isExpect = true)
}

internal fun ClassDeclaration.buildDescriptor(
    output: CommonizedGroup<in ClassifierDescriptorWithTypeParameters>,
    index: Int,
    containingDeclarations: List<DeclarationDescriptor?>,
    storageManager: StorageManager,
    isExpect: Boolean = false,
    isActual: Boolean = false
) {
    val containingDeclaration = containingDeclarations[index] ?: error("No containing declaration for class $this")

    val classDescriptor = CommonizedClassDescriptor(
        storageManager = storageManager,
        containingDeclaration = containingDeclaration,
        annotations = annotations,
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
        companionObjectName = companion?.shortName(),
        supertypes = supertypes
    )

    classDescriptor.declaredTypeParameters = typeParameters.buildDescriptors(classDescriptor)

    output[index] = classDescriptor
}

internal fun ClassConstructorNode.buildDescriptors(
    output: CommonizedGroup<ClassConstructorDescriptor>,
    containingDeclarations: List<ClassDescriptor?>
) {
    val commonConstructor = common()
    val markAsActual = commonConstructor != null

    target.forEachIndexed { index, constructor ->
        constructor?.buildDescriptor(output, index, containingDeclarations, isActual = markAsActual)
    }

    commonConstructor?.buildDescriptor(output, indexOfCommon, containingDeclarations, isExpect = true)
}

private fun ClassConstructor.buildDescriptor(
    output: CommonizedGroup<ClassConstructorDescriptor>,
    index: Int,
    containingDeclarations: List<ClassDescriptor?>,
    isExpect: Boolean = false,
    isActual: Boolean = false
) {
    val containingDeclaration = containingDeclarations[index] ?: error("No containing declaration for constructor $this")

    val constructorDescriptor = CommonizedClassConstructorDescriptor(
        containingDeclaration = containingDeclaration,
        annotations = annotations,
        isPrimary = isPrimary,
        kind = kind
    )

    constructorDescriptor.isExpect = isExpect
    constructorDescriptor.isActual = isActual

    constructorDescriptor.setHasStableParameterNames(hasStableParameterNames)
    constructorDescriptor.setHasSynthesizedParameterNames(hasSynthesizedParameterNames)

    constructorDescriptor.initialize(
        valueParameters.buildDescriptors(constructorDescriptor),
        visibility,
        typeParameters.buildDescriptors(constructorDescriptor)
    )

    output[index] = constructorDescriptor
}
