/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.cir.impl

import org.jetbrains.kotlin.descriptors.commonizer.cir.CirAnnotation
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirType
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirValueParameter
import org.jetbrains.kotlin.descriptors.commonizer.utils.appendHashCode
import org.jetbrains.kotlin.descriptors.commonizer.utils.hashCode
import org.jetbrains.kotlin.name.Name

data class CirValueParameterImpl(
    override val annotations: List<CirAnnotation>,
    override val name: Name,
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
}
