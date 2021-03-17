/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.cir

import org.jetbrains.kotlin.commonizer.utils.Interner
import org.jetbrains.kotlin.commonizer.utils.appendHashCode
import org.jetbrains.kotlin.commonizer.utils.hashCode

interface CirValueParameter : CirHasAnnotations, CirHasName {
    val returnType: CirType
    val varargElementType: CirType?
    val declaresDefaultValue: Boolean
    val isCrossinline: Boolean
    val isNoinline: Boolean

    companion object {
        fun createInterned(
            annotations: List<CirAnnotation>,
            name: CirName,
            returnType: CirType,
            varargElementType: CirType?,
            declaresDefaultValue: Boolean,
            isCrossinline: Boolean,
            isNoinline: Boolean
        ): CirValueParameter = interner.intern(
            CirValueParameterInternedImpl(
                annotations = annotations,
                name = name,
                returnType = returnType,
                varargElementType = varargElementType,
                declaresDefaultValue = declaresDefaultValue,
                isCrossinline = isCrossinline,
                isNoinline = isNoinline
            )
        )

        private val interner = Interner<CirValueParameterInternedImpl>()
    }
}

private class CirValueParameterInternedImpl(
    override val annotations: List<CirAnnotation>,
    override val name: CirName,
    override val returnType: CirType,
    override val varargElementType: CirType?,
    override val declaresDefaultValue: Boolean,
    override val isCrossinline: Boolean,
    override val isNoinline: Boolean
) : CirValueParameter {
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

    override fun equals(other: Any?) = when {
        other === this -> true
        other is CirValueParameterInternedImpl -> {
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

    override fun toString() = buildString {
        if (varargElementType != null) append("vararg ")
        append(name)
        append(": ")
        append(returnType)
    }
}
