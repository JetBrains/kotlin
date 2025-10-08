/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.parser.antlr

import org.jetbrains.kotlin.js.parser.JsParserException
import org.jetbrains.kotlin.js.parser.CodePosition
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.tree.ParseTree
import org.jetbrains.kotlin.js.parser.antlr.generated.JavaScriptParserBaseVisitor

internal abstract class AntlrJsBaseVisitor<TResult>() : JavaScriptParserBaseVisitor<TResult>() {
    protected inline fun <reified T> visitNode(node: ParseTree): T =
        visit(node).expect<T>()

    protected inline fun <reified T> visitAll(nodes: List<ParseTree>): List<T> =
        nodes.map { visitNode<T>(it) }

    protected inline fun <reified T> TResult?.expect(): T {
        if (this !is T) raiseParserException("Expected ${T::class}, got ${this?.javaClass}")
        return this
    }

    protected fun raiseParserException(message: String, rule: ParserRuleContext? = null): Nothing {
        throw JsParserException("Parser encountered internal error: $message", rule?.startPosition ?: CodePosition(0, 0))
    }
}