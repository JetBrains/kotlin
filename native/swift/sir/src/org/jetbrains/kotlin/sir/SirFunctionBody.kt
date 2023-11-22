/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir

class SirFunctionBody(val bridgeName: String, private val parameters: List<SirParameter>) {
    fun generateCallSite(): String {
        val arguments = parameters.map { it.parameterName ?: it.argumentName }
        return "$bridgeName(${arguments.joinToString(separator = ", ")})"
    }
}