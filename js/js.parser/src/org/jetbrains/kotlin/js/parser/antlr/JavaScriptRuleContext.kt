/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.parser.antlr

import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.Token

abstract class JavaScriptRuleContext : ParserRuleContext {
    constructor(parent: ParserRuleContext?, invokingStateNumber: Int) : super(parent, invokingStateNumber)
    constructor() : super()

    val commentsBefore = mutableListOf<Token>()
    val commentsAfter = mutableListOf<Token>()
}