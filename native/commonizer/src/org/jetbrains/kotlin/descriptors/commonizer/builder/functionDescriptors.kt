/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.builder

import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.CirFunction
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.CirFunctionNode
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.indexOfCommon
import org.jetbrains.kotlin.descriptors.commonizer.utils.CommonizedGroup
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl

internal fun CirFunctionNode.buildDescriptors(
    components: GlobalDeclarationsBuilderComponents,
    output: CommonizedGroup<SimpleFunctionDescriptor>,
    containingDeclarations: List<DeclarationDescriptor?>
) {
    val commonFunction = common()
    val markAsExpectAndActual = commonFunction != null && commonFunction.kind != CallableMemberDescriptor.Kind.SYNTHESIZED

    target.forEachIndexed { index, function ->
        function?.buildDescriptor(components, output, index, containingDeclarations, isActual = markAsExpectAndActual)
    }

    commonFunction?.buildDescriptor(components, output, indexOfCommon, containingDeclarations, isExpect = markAsExpectAndActual)

    // log stats
    components.statsCollector?.logStats(output.toList())
}

private fun CirFunction.buildDescriptor(
    components: GlobalDeclarationsBuilderComponents,
    output: CommonizedGroup<SimpleFunctionDescriptor>,
    index: Int,
    containingDeclarations: List<DeclarationDescriptor?>,
    isExpect: Boolean = false,
    isActual: Boolean = false
) {
    val targetComponents = components.targetComponents[index]
    val containingDeclaration = containingDeclarations[index] ?: error("No containing declaration for function $this")

    val functionDescriptor = SimpleFunctionDescriptorImpl.create(
        containingDeclaration,
        annotations.buildDescriptors(targetComponents),
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

    val (typeParameters, typeParameterResolver) = typeParameters.buildDescriptorsAndTypeParameterResolver(
        targetComponents,
        containingDeclaration.getTypeParameterResolver(),
        functionDescriptor
    )

    functionDescriptor.initialize(
        extensionReceiver?.buildExtensionReceiver(targetComponents, typeParameterResolver, functionDescriptor),
        buildDispatchReceiver(functionDescriptor),
        typeParameters,
        valueParameters.buildDescriptors(targetComponents, typeParameterResolver, functionDescriptor),
        returnType.buildType(targetComponents, typeParameterResolver),
        modality,
        visibility
    )

    output[index] = functionDescriptor
}
