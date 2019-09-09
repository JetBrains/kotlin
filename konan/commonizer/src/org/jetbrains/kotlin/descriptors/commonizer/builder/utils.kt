/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.builder

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.ExtensionReceiver
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.TypeParameter
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.ValueParameter
import org.jetbrains.kotlin.descriptors.impl.TypeParameterDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.resolve.DescriptorFactory
import org.jetbrains.kotlin.resolve.DescriptorUtils

internal fun List<TypeParameter>.buildDescriptors(
    containingDeclaration: DeclarationDescriptor
): List<TypeParameterDescriptor> {
    return mapIndexed { index, param ->
        val descriptor = TypeParameterDescriptorImpl.createForFurtherModification(
            containingDeclaration,
            param.annotations,
            param.isReified,
            param.variance,
            param.name,
            index,
            SourceElement.NO_SOURCE
        )

        param.upperBounds.forEach(descriptor::addUpperBound)
        descriptor.setInitialized()

        descriptor
    }
}

internal fun List<ValueParameter>.buildDescriptors(
    containingDeclaration: CallableDescriptor
) = mapIndexed { index, param ->
    ValueParameterDescriptorImpl(
        containingDeclaration,
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

internal fun ExtensionReceiver.buildExtensionReceiver(
    containingDeclaration: CallableDescriptor
) = DescriptorFactory.createExtensionReceiverParameterForCallable(
    containingDeclaration,
    type,
    annotations
)

internal fun buildDispatchReceiver(callableDescriptor: CallableDescriptor) =
    DescriptorUtils.getDispatchReceiverParameterIfNeeded(callableDescriptor.containingDeclaration)
