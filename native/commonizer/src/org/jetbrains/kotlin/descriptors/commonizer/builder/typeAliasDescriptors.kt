/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.builder

import org.jetbrains.kotlin.descriptors.ClassifierDescriptorWithTypeParameters
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.CirClass
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.CirTypeAlias
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.CirTypeAliasNode
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.indexOfCommon
import org.jetbrains.kotlin.descriptors.commonizer.utils.CommonizedGroup
import org.jetbrains.kotlin.name.FqName

internal fun CirTypeAliasNode.buildDescriptors(
    components: GlobalDeclarationsBuilderComponents,
    output: CommonizedGroup<ClassifierDescriptorWithTypeParameters>,
    containingDeclarations: List<DeclarationDescriptor?>
) {
    val commonClass: CirClass? = common()
    val markAsActual = commonClass != null

    target.forEachIndexed { index, typeAlias ->
        typeAlias?.buildDescriptor(components, output, index, containingDeclarations, fqName, isActual = markAsActual)
    }

    commonClass?.buildDescriptor(components, output, indexOfCommon, containingDeclarations, fqName, isExpect = true)

    // log stats
    components.statsCollector?.logStats(output.toList())
}

private fun CirTypeAlias.buildDescriptor(
    components: GlobalDeclarationsBuilderComponents,
    output: CommonizedGroup<ClassifierDescriptorWithTypeParameters>,
    index: Int,
    containingDeclarations: List<DeclarationDescriptor?>,
    fqName: FqName,
    isActual: Boolean = false
) {
    val targetComponents = components.targetComponents[index]
    val storageManager = targetComponents.storageManager
    val containingDeclaration = containingDeclarations[index] ?: error("No containing declaration for type alias $this")

    val typeAliasDescriptor = CommonizedTypeAliasDescriptor(
        storageManager = storageManager,
        containingDeclaration = containingDeclaration,
        annotations = annotations.buildDescriptors(targetComponents),
        name = name,
        visibility = visibility,
        isActual = isActual
    )

    val (declaredTypeParameters, typeParameterResolver) = typeParameters.buildDescriptorsAndTypeParameterResolver(
        targetComponents,
        TypeParameterResolver.EMPTY,
        typeAliasDescriptor
    )

    typeAliasDescriptor.initialize(
        declaredTypeParameters = declaredTypeParameters,
        underlyingType = storageManager.createLazyValue { underlyingType.buildType(targetComponents, typeParameterResolver) },
        expandedType = storageManager.createLazyValue { expandedType.buildType(targetComponents, typeParameterResolver) }
    )

    // cache created type alias descriptor:
    components.cache.cache(fqName, index, typeAliasDescriptor)

    output[index] = typeAliasDescriptor
}
