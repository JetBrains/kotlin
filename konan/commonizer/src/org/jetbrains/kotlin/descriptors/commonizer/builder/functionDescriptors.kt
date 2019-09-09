/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.builder

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.commonizer.CommonizedGroup
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.Function
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.FunctionNode
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.indexOfCommon
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl

internal fun FunctionNode.buildDescriptors(
    output: CommonizedGroup<SimpleFunctionDescriptor>,
    containingDeclarations: List<DeclarationDescriptor?>
) {
    val commonFunction = common()

    target.forEachIndexed { index, function ->
        function?.buildDescriptor(output, index, containingDeclarations, isActual = commonFunction != null)
    }

    commonFunction?.buildDescriptor(output, indexOfCommon, containingDeclarations, isExpect = true)
}

private fun Function.buildDescriptor(
    output: CommonizedGroup<SimpleFunctionDescriptor>,
    index: Int,
    containingDeclarations: List<DeclarationDescriptor?>,
    isExpect: Boolean = false,
    isActual: Boolean = false
) {
    val containingDeclaration = containingDeclarations[index] ?: error("No containing declaration for function $this")

    val functionDescriptor = SimpleFunctionDescriptorImpl.create(
        containingDeclaration,
        annotations,
        name,
        kind,
        SourceElement.NO_SOURCE
    )

    functionDescriptor.isOperator = isOperator
    functionDescriptor.isInfix = isInfix
    functionDescriptor.isInline = isInline
    functionDescriptor.isTailrec = isTailrec
    functionDescriptor.isSuspend = isSuspend
    functionDescriptor.isExternal = isExternal

    functionDescriptor.isExpect = isExpect
    functionDescriptor.isActual = isActual

    functionDescriptor.setHasStableParameterNames(hasStableParameterNames)
    functionDescriptor.setHasSynthesizedParameterNames(hasSynthesizedParameterNames)

    functionDescriptor.initialize(
        extensionReceiver?.buildExtensionReceiver(functionDescriptor),
        buildDispatchReceiver(functionDescriptor),
        typeParameters.buildDescriptors(functionDescriptor),
        valueParameters.buildDescriptors(functionDescriptor),
        returnType,
        modality,
        visibility
    )

    output[index] = functionDescriptor
}
