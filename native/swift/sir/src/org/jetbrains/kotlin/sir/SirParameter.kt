/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir

data class SirParameter(
    val argumentName: String? = null, // external function parameter (argument) name
    val parameterName: String? = null, // internal function parameter name
    val type: SirType,
    /* TODO:  val defaultValue: Expression? = null, */
)