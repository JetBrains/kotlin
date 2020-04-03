/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.commonizer.utils.Interner
import org.jetbrains.kotlin.descriptors.commonizer.utils.hashCode
import org.jetbrains.kotlin.descriptors.commonizer.utils.appendHashCode
import org.jetbrains.kotlin.descriptors.commonizer.utils.intern
import org.jetbrains.kotlin.name.Name

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

class CirFunctionImpl(original: SimpleFunctionDescriptor) : CirFunctionOrPropertyImpl<SimpleFunctionDescriptor>(original), CirFunction {
    override val isOperator = original.isOperator
    override val isInfix = original.isInfix
    override val isInline = original.isInline
    override val isTailrec = original.isTailrec
    override val isSuspend = original.isSuspend
    override val valueParameters = original.valueParameters.map(CirValueParameterImpl.Companion::create)
    override val hasStableParameterNames = original.hasStableParameterNames()
    override val hasSynthesizedParameterNames = original.hasSynthesizedParameterNames()
}

interface CirValueParameter {
    val name: Name
    val annotations: List<CirAnnotation>
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
    override val annotations: List<CirAnnotation> get() = emptyList()
    override val declaresDefaultValue get() = false
}

class CirValueParameterImpl private constructor(original: ValueParameterDescriptor) : CirValueParameter {
    override val name = original.name.intern()
    override val annotations = original.annotations.map(CirAnnotation.Companion::create)
    override val returnType = CirType.create(original.returnType!!)
    override val varargElementType = original.varargElementType?.let(CirType.Companion::create)
    override val declaresDefaultValue = original.declaresDefaultValue()
    override val isCrossinline = original.isCrossinline
    override val isNoinline = original.isNoinline

    // See also org.jetbrains.kotlin.types.KotlinType.cachedHashCode
    private var cachedHashCode = 0

    private fun computeHashCode() = hashCode(name)
        .appendHashCode(annotations)
        .appendHashCode(returnType)
        .appendHashCode(varargElementType)
        .appendHashCode(declaresDefaultValue)
        .appendHashCode(isCrossinline)
        .appendHashCode(isNoinline)

    override fun hashCode(): Int {
        var currentHashCode = cachedHashCode
        if (currentHashCode != 0) return currentHashCode

        currentHashCode = computeHashCode()
        cachedHashCode = currentHashCode
        return currentHashCode
    }

    override fun equals(other: Any?): Boolean = when {
        other === this -> true
        other is CirValueParameterImpl -> {
            name == other.name
                    && returnType == other.returnType
                    && annotations == other.annotations
                    && varargElementType == other.varargElementType
                    && declaresDefaultValue == other.declaresDefaultValue
                    && isCrossinline == other.isCrossinline
                    && isNoinline == other.isNoinline
        }
        else -> false
    }

    companion object {
        private val interner = Interner<CirValueParameterImpl>()

        fun create(original: ValueParameterDescriptor): CirValueParameterImpl = interner.intern(CirValueParameterImpl(original))
    }
}
