/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.ir

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.UnwrappedType

interface FunctionModifiers {
    val isOperator: Boolean
    val isInfix: Boolean
    val isInline: Boolean
    val isTailrec: Boolean
    val isSuspend: Boolean
    val isExternal: Boolean
}

interface Function : CallableMember, FunctionModifiers, Declaration {
    val valueParameters: List<ValueParameter>
}

data class CommonFunction(
    override val name: Name,
    override val modality: Modality,
    override val visibility: Visibility,
    override val extensionReceiver: ExtensionReceiver?,
    override val returnType: UnwrappedType,
    private val modifiers: FunctionModifiers,
    override val valueParameters: List<ValueParameter>
) : CommonCallableMember(), Function, FunctionModifiers by modifiers

class TargetFunction(descriptor: SimpleFunctionDescriptor) : TargetCallableMember<SimpleFunctionDescriptor>(descriptor), Function {
    override val isOperator: Boolean get() = descriptor.isOperator
    override val isInfix: Boolean get() = descriptor.isInfix
    override val isInline: Boolean get() = descriptor.isInline
    override val isTailrec: Boolean get() = descriptor.isTailrec
    override val isSuspend: Boolean get() = descriptor.isSuspend
    override val valueParameters: List<ValueParameter> get() = descriptor.valueParameters.map(::PlatformValueParameter)
}

interface ValueParameter {
    val name: Name
    val annotations: Annotations
    val returnType: UnwrappedType
    val varargElementType: UnwrappedType?
    val declaresDefaultValue: Boolean
    val isCrossinline: Boolean
    val isNoinline: Boolean
}

data class CommonValueParameter(
    override val name: Name,
    override val returnType: UnwrappedType,
    override val varargElementType: UnwrappedType?,
    override val isCrossinline: Boolean,
    override val isNoinline: Boolean
) : ValueParameter {
    override val annotations: Annotations get() = Annotations.EMPTY
    override val declaresDefaultValue: Boolean get() = false
}

data class PlatformValueParameter(private val descriptor: ValueParameterDescriptor) : ValueParameter {
    override val name: Name get() = descriptor.name
    override val annotations: Annotations get() = descriptor.annotations
    override val returnType: UnwrappedType get() = descriptor.returnType!!.unwrap()
    override val varargElementType: UnwrappedType? get() = descriptor.varargElementType?.unwrap()
    override val declaresDefaultValue: Boolean get() = descriptor.declaresDefaultValue()
    override val isCrossinline: Boolean get() = descriptor.isCrossinline
    override val isNoinline: Boolean get() = descriptor.isNoinline
}
