/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.builder

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.commonizer.CommonizedGroup
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.Function
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.FunctionNode
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.ValueParameter
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.indexOfCommon
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.resolve.DescriptorFactory
import org.jetbrains.kotlin.resolve.DescriptorUtils

internal fun FunctionNode.buildDescriptors(
    output: CommonizedGroup<SimpleFunctionDescriptor>,
    containingDeclarations: List<DeclarationDescriptor?>
) {
    val isCommonized = common != null

    target.forEachIndexed { index, function ->
        function?.buildDescriptor(output, index, containingDeclarations, isActual = isCommonized)
    }

    common?.buildDescriptor(output, indexOfCommon, containingDeclarations, isExpect = isCommonized)
}

private fun Function.buildDescriptor(
    output: CommonizedGroup<SimpleFunctionDescriptor>,
    index: Int,
    containingDeclarations: List<DeclarationDescriptor?>,
    isExpect: Boolean = false,
    isActual: Boolean = false
) {
    val containingDeclaration = containingDeclarations[index] ?: error("No containing declaration for property $this")

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

    val extensionReceiverDescriptor = DescriptorFactory.createExtensionReceiverParameterForCallable(
        functionDescriptor,
        extensionReceiver?.type,
        extensionReceiver?.annotations ?: Annotations.EMPTY
    )

    val dispatchReceiverDescriptor = DescriptorUtils.getDispatchReceiverParameterIfNeeded(containingDeclaration)

    functionDescriptor.initialize(
        extensionReceiverDescriptor,
        dispatchReceiverDescriptor,
        emptyList(), // TODO: support type parameters
        valueParameters.buildValueParameters(functionDescriptor),
        returnType,
        modality,
        visibility
    )

    output[index] = functionDescriptor
}

private fun List<ValueParameter>.buildValueParameters(
    functionDescriptor: SimpleFunctionDescriptor
) = mapIndexed { index, param ->
    ValueParameterDescriptorImpl(
        functionDescriptor,
        null,
        index,
        param.annotations,
        param.name,
        param.returnType,
        param.declaresDefaultValue,
        param.isCrossinline,
        param.isNoinline,
        param.varargElementType,
        SourceElement.NO_SOURCE
    )
}
