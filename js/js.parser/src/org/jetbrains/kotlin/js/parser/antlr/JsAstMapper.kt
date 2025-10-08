/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.parser.antlr

import org.jetbrains.kotlin.js.parser.ScopeContext
import org.jetbrains.kotlin.js.parser.JsParserException
import org.jetbrains.kotlin.js.parser.ErrorReporter
import org.antlr.v4.runtime.ParserRuleContext
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.parser.antlr.generated.JavaScriptParser

internal class JsAstMapper(scope: JsScope, private val fileName: String, private val reporter: ErrorReporter) {
    companion object {
        fun createParserException(message: String, ctx: ParserRuleContext): JsParserException {
            return JsParserException("Parser encountered internal error: $message", ctx.startPosition)
        }
    }

    private val scopeContext = ScopeContext(scope)

    fun mapStatement(statement: JavaScriptParser.StatementContext): JsStatement {
        val jsStatement = map(statement)
        if (jsStatement !is JsStatement)
            throw createParserException("Expecting a statement", statement)

        return jsStatement
    }

    fun mapFunction(function: JavaScriptParser.FunctionDeclarationContext): JsFunction {
        val jsFunction = map(function)
        if (jsFunction !is JsFunction)
            throw createParserException("Expecting a function", function)

        return jsFunction
    }

    fun mapExpression(expression: ParserRuleContext): JsExpression {
        val jsExpression = map(expression)
        if (jsExpression !is JsExpression)
            throw createParserException("Expecting an expression", expression)

        return jsExpression
    }

    private fun map(node: ParserRuleContext): JsNode {
        val visitor = JsAstMapperVisitor(fileName, scopeContext, reporter)
        return node.accept(visitor)!!
    }
}