/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.cir

import org.jetbrains.kotlin.commonizer.utils.Interner
import org.jetbrains.kotlin.commonizer.utils.appendHashCode
import org.jetbrains.kotlin.commonizer.utils.hashCode

class CirValueParameter private constructor(
    override val annotations: List<CirAnnotation>,
    override val name: CirName,
    val returnType: CirType,
    val varargElementType: CirType?,
    val declaresDefaultValue: Boolean,
    val isCrossinline: Boolean,
    val isNoinline: Boolean
) : CirHasAnnotations, CirHasName {
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
        other is CirValueParameter -> {
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

    companion object {
        private val interner = Interner<CirValueParameter>()

        fun createInterned(
            annotations: List<CirAnnotation>,
            name: CirName,
            returnType: CirType,
            varargElementType: CirType?,
            declaresDefaultValue: Boolean,
            isCrossinline: Boolean,
            isNoinline: Boolean
        ): CirValueParameter = interner.intern(
            CirValueParameter(
                annotations = annotations,
                name = name,
                returnType = returnType,
                varargElementType = varargElementType,
                declaresDefaultValue = declaresDefaultValue,
                isCrossinline = isCrossinline,
                isNoinline = isNoinline
            )
        )

        fun CirValueParameter.copyInterned(
            annotations: List<CirAnnotation> = this.annotations,
            name: CirName = this.name,
            returnType: CirType = this.returnType,
            varargElementType: CirType? = this.varargElementType,
            declaresDefaultValue: Boolean = this.declaresDefaultValue,
            isCrossinline: Boolean = this.isCrossinline,
            isNoinline: Boolean = this.isNoinline
        ): CirValueParameter = createInterned(
            annotations = annotations,
            name = name,
            returnType = returnType,
            varargElementType = varargElementType,
            declaresDefaultValue = declaresDefaultValue,
            isCrossinline = isCrossinline,
            isNoinline = isNoinline
        )
    }

}
