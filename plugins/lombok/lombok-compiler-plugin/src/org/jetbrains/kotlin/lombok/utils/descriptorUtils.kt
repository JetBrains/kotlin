/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.utils

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.ClassConstructorDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.KotlinType

internal data class ValueParameter(val name: Name, val type: KotlinType)

internal fun ClassDescriptor.createFunction(
    name: Name,
    valueParameters: List<ValueParameter>,
    returnType: KotlinType?,
    modality: Modality? = Modality.OPEN,
    visibility: DescriptorVisibility = DescriptorVisibilities.PUBLIC,
    receiver: ReceiverParameterDescriptor? = this.thisAsReceiverParameter
): SimpleFunctionDescriptor {
    val methodDescriptor = SimpleFunctionDescriptorImpl.create(
        this,
        Annotations.EMPTY,
        name,
        CallableMemberDescriptor.Kind.SYNTHESIZED,
        this.source
    )

    val paramDescriptors = valueParameters.mapIndexed { idx, param -> methodDescriptor.makeValueParameter(param, idx) }

    methodDescriptor.initialize(
        null,
        receiver,
        mutableListOf(),
        paramDescriptors,
        returnType,
        modality,
        visibility
    )
    return methodDescriptor
}

internal fun ClassDescriptor.createConstructor(
    valueParameters: List<ValueParameter>,
    visibility: DescriptorVisibility = DescriptorVisibilities.PUBLIC
): ClassConstructorDescriptor {
    val constructor = ClassConstructorDescriptorImpl.create(
        this,
        Annotations.EMPTY,
        false,
        this.source
    )
    val paramDescriptors = valueParameters.mapIndexed { idx, param -> constructor.makeValueParameter(param, idx) }
    constructor.initialize(
        null,
        constructor.calculateDispatchReceiverParameter(),
        emptyList(),
        paramDescriptors,
        this.defaultType,
        Modality.OPEN,
        visibility
    )
    return constructor
}

private fun CallableDescriptor.makeValueParameter(param: ValueParameter, index: Int): ValueParameterDescriptor {
    return ValueParameterDescriptorImpl(
        containingDeclaration = this,
        original = null,
        index = index,
        annotations = Annotations.EMPTY,
        name = param.name,
        outType = param.type,
        declaresDefaultValue = false,
        isCrossinline = false,
        isNoinline = false,
        varargElementType = null,
        source = this.source
    )
}

internal fun ClassDescriptor.getVariables(): List<PropertyDescriptor> =
    this.unsubstitutedMemberScope.getVariableNames()
        .map {
            this.unsubstitutedMemberScope.getContributedVariables(it, NoLookupLocation.FROM_SYNTHETIC_SCOPE).single()
        }
