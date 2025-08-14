/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.parser.antlr

import com.google.gwt.dev.js.ScopeContext
import com.google.gwt.dev.js.parserExceptions.JsParserException
import org.antlr.v4.runtime.ParserRuleContext
import org.jetbrains.kotlin.js.backend.ast.*

class JsAstMapper(private val scope: JsScope, private val fileName: String) {
    companion object {
        private fun createParserException(message: String, ctx: ParserRuleContext): JsParserException {
            return JsParserException("Parser encountered internal error: $message", ctx.startPosition)
        }
    }

    private val scopeContext = ScopeContext(scope)

    public fun mapStatement(statement: ParserRuleContext): JsStatement? {
        TODO("mapStatement")
    }

    public fun mapFunction(function: ParserRuleContext): JsFunction? {
        TODO("mapFunction")
    }

    public fun mapExpression(expression: ParserRuleContext): JsExpression {
        val targetExpr = map(expression)
        if (targetExpr !is JsExpression)
            throw createParserException("Expecting an expression", expression)

        return targetExpr
    }

    private fun map(node: ParserRuleContext): JsNode {
        return mapWithoutLocation(node).applyLocation(fileName, node)
    }

    private fun mapWithoutLocation(node: ParserRuleContext): JsNode {
        val visitor = JsAstMapperVisitor(fileName, scopeContext)
        return node.accept(visitor)!!
    }

    private
}