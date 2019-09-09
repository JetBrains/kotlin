/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.builder

import org.jetbrains.kotlin.descriptors.ClassifierDescriptorWithTypeParameters
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.commonizer.CommonizedGroup
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.ClassDeclaration
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.TypeAlias
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.TypeAliasNode
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.indexOfCommon
import org.jetbrains.kotlin.storage.StorageManager

internal fun TypeAliasNode.buildDescriptors(
    output: CommonizedGroup<ClassifierDescriptorWithTypeParameters>,
    containingDeclarations: List<DeclarationDescriptor?>,
    storageManager: StorageManager
) {
    val commonClass: ClassDeclaration? = common()

    target.forEachIndexed { index, typeAlias ->
        typeAlias?.buildDescriptor(output, index, containingDeclarations, storageManager, isActual = commonClass != null)
    }

    commonClass?.buildDescriptor(output, indexOfCommon, containingDeclarations, storageManager, isExpect = true)
}

private fun TypeAlias.buildDescriptor(
    output: CommonizedGroup<ClassifierDescriptorWithTypeParameters>,
    index: Int,
    containingDeclarations: List<DeclarationDescriptor?>,
    storageManager: StorageManager,
    isActual: Boolean = false
) {
    val containingDeclaration = containingDeclarations[index] ?: error("No containing declaration for type alias $this")

    val typeAliasDescriptor = CommonizedTypeAliasDescriptor(
        storageManager = storageManager,
        containingDeclaration = containingDeclaration,
        annotations = annotations,
        name = name,
        visibility = visibility,
        isActual = isActual
    )

    typeAliasDescriptor.initialize(underlyingType, expandedType)

    output[index] = typeAliasDescriptor
}
