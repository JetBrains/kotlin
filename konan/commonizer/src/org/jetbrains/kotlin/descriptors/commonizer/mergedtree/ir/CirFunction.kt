/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.name.Name
import kotlin.LazyThreadSafetyMode.PUBLICATION

interface CirFunctionModifiers {
    val isOperator: Boolean
    val isInfix: Boolean
    val isInline: Boolean
    val isTailrec: Boolean
    val isSuspend: Boolean
    val isExternal: Boolean
}

interface CirCallableMemberWithParameters {
    val valueParameters: List<CirValueParameter>
    val hasStableParameterNames: Boolean
    val hasSynthesizedParameterNames: Boolean
}

interface CirFunction : CirFunctionOrProperty, CirFunctionModifiers, CirCallableMemberWithParameters

data class CirCommonFunction(
    override val name: Name,
    override val modality: Modality,
    override val visibility: Visibility,
    override val extensionReceiver: CirExtensionReceiver?,
    override val returnType: CirType,
    override val kind: CallableMemberDescriptor.Kind,
    private val modifiers: CirFunctionModifiers,
    override val valueParameters: List<CirValueParameter>,
    override val typeParameters: List<CirTypeParameter>,
    override val hasStableParameterNames: Boolean,
    override val hasSynthesizedParameterNames: Boolean
) : CirCommonFunctionOrProperty(), CirFunction, CirFunctionModifiers by modifiers

class CirWrappedFunction(wrapped: SimpleFunctionDescriptor) : CirWrappedFunctionOrProperty<SimpleFunctionDescriptor>(wrapped), CirFunction {
    override val isOperator get() = wrapped.isOperator
    override val isInfix get() = wrapped.isInfix
    override val isInline get() = wrapped.isInline
    override val isTailrec get() = wrapped.isTailrec
    override val isSuspend get() = wrapped.isSuspend
    override val valueParameters by lazy(PUBLICATION) { wrapped.valueParameters.map(::CirWrappedValueParameter) }
    override val hasStableParameterNames get() = wrapped.hasStableParameterNames()
    override val hasSynthesizedParameterNames get() = wrapped.hasSynthesizedParameterNames()
}

interface CirValueParameter {
    val name: Name
    val annotations: Annotations
    val returnType: CirType
    val varargElementType: CirType?
    val declaresDefaultValue: Boolean
    val isCrossinline: Boolean
    val isNoinline: Boolean
}

data class CirCommonValueParameter(
    override val name: Name,
    override val returnType: CirType,
    override val varargElementType: CirType?,
    override val isCrossinline: Boolean,
    override val isNoinline: Boolean
) : CirValueParameter {
    override val annotations get() = Annotations.EMPTY
    override val declaresDefaultValue get() = false
}

data class CirWrappedValueParameter(private val wrapped: ValueParameterDescriptor) : CirValueParameter {
    override val name get() = wrapped.name
    override val annotations get() = wrapped.annotations
    override val returnType by lazy(PUBLICATION) { CirType.create(wrapped.returnType!!) }
    override val varargElementType by lazy(PUBLICATION) { wrapped.varargElementType?.let(CirType.Companion::create) }
    override val declaresDefaultValue get() = wrapped.declaresDefaultValue()
    override val isCrossinline get() = wrapped.isCrossinline
    override val isNoinline get() = wrapped.isNoinline
}
