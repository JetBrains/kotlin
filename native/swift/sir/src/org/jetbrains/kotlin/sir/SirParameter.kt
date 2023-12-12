/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir

class SirParameter(
    val argumentName: String? = null, // external function parameter (argument) name
    val parameterName: String? = null, // internal function parameter name
    val type: SirType,
    /* TODO:  val defaultValue: Expression? = null, */
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other != null && this::class != other::class) return false

        other as SirParameter

        if (argumentName != other.argumentName) return false
        if (parameterName != other.parameterName) return false
        if (type != other.type) return false

        return true
    }

    override fun hashCode(): Int {
        var result = argumentName?.hashCode() ?: 0
        result = 31 * result + (parameterName?.hashCode() ?: 0)
        result = 31 * result + type.hashCode()
        return result
    }
}