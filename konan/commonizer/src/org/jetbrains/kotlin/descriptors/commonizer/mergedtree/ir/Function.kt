/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.UnwrappedType
import kotlin.LazyThreadSafetyMode.PUBLICATION

interface FunctionModifiers {
    val isOperator: Boolean
    val isInfix: Boolean
    val isInline: Boolean
    val isTailrec: Boolean
    val isSuspend: Boolean
    val isExternal: Boolean
}

interface CallableMemberWithParameters {
    val valueParameters: List<ValueParameter>
    val hasStableParameterNames: Boolean
    val hasSynthesizedParameterNames: Boolean
}

interface Function : FunctionOrProperty, FunctionModifiers, CallableMemberWithParameters

data class CommonFunction(
    override val name: Name,
    override val modality: Modality,
    override val visibility: Visibility,
    override val extensionReceiver: ExtensionReceiver?,
    override val returnType: UnwrappedType,
    private val modifiers: FunctionModifiers,
    override val valueParameters: List<ValueParameter>,
    override val typeParameters: List<TypeParameter>,
    override val hasStableParameterNames: Boolean,
    override val hasSynthesizedParameterNames: Boolean
) : CommonFunctionOrProperty(), Function, FunctionModifiers by modifiers

class TargetFunction(descriptor: SimpleFunctionDescriptor) : TargetFunctionOrProperty<SimpleFunctionDescriptor>(descriptor), Function {
    override val isOperator get() = descriptor.isOperator
    override val isInfix get() = descriptor.isInfix
    override val isInline get() = descriptor.isInline
    override val isTailrec get() = descriptor.isTailrec
    override val isSuspend get() = descriptor.isSuspend
    override val valueParameters by lazy(PUBLICATION) { descriptor.valueParameters.map(::PlatformValueParameter) }
    override val hasStableParameterNames: Boolean get() = descriptor.hasStableParameterNames()
    override val hasSynthesizedParameterNames: Boolean get() = descriptor.hasSynthesizedParameterNames()
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
    override val annotations get() = Annotations.EMPTY
    override val declaresDefaultValue get() = false
}

data class PlatformValueParameter(private val descriptor: ValueParameterDescriptor) : ValueParameter {
    override val name get() = descriptor.name
    override val annotations get() = descriptor.annotations
    override val returnType by lazy(PUBLICATION) { descriptor.returnType!!.unwrap() }
    override val varargElementType by lazy(PUBLICATION) { descriptor.varargElementType?.unwrap() }
    override val declaresDefaultValue get() = descriptor.declaresDefaultValue()
    override val isCrossinline get() = descriptor.isCrossinline
    override val isNoinline get() = descriptor.isNoinline
}
