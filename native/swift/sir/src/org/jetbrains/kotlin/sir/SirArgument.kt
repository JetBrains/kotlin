/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir

class SirArgument(
    val name: String? = null, // null indicates positional (unnamed) argument
    val expression: SirExpression
) {
    constructor(name: String?, expression: String) : this(name, SirExpression(expression))
    constructor(expression: String) : this(null, SirExpression(expression))
}

class SirExpression(val raw: String) {
    override fun toString(): String = raw
}