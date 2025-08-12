/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.parser.antlr

import com.google.gwt.dev.js.parserExceptions.JsParserException
import org.antlr.v4.runtime.ParserRuleContext
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.parser.antlr.generated.JavaScriptParser

class JsAstMapper(private val scope: JsScope, private val fileName: String) {
    companion object {
        private fun createParserException(message: String, ctx: ParserRuleContext): JsParserException {
            return JsParserException("Parser encountered internal error: $message", ctx.startPosition)
        }
    }

    public fun mapStatement(statement: ParserRuleContext): JsStatement? {
        TODO("mapStatement")
    }

    public fun mapFunction(function: ParserRuleContext): JsFunction? {
        TODO("mapFunction")
    }

    public fun mapExpression(expression: ParserRuleContext): JsExpression? {
        val targetExpr = map(expression)
        if (targetExpr !is JsExpression)
            throw createParserException("Expecting an expression", expression)

        return targetExpr
    }

    private fun map(node: ParserRuleContext): JsNode {
        return mapWithoutLocation(node).applyLocation(node)
    }

    private fun mapWithoutLocation(node: ParserRuleContext): JsNode {
        val visitor = JsAstMapperVisitor()
        return node.accept(visitor)
    }

    private fun <T : JsNode> T.applyLocation(sourceNode: ParserRuleContext): T =
        this.also { targetNode ->
            val location = when (sourceNode) {
                is JavaScriptParser.FunctionDeclarationContext ->
                    // For functions, consider their location to be at the opening parenthesis.
                    sourceNode.OpenParen().symbol.codePosition
                is JavaScriptParser.MemberDotExpressionContext ->
                    // For dot-qualified references, consider their position to be at the rightmost name reference.
                    sourceNode.identifierName().startPosition
                else ->
                    sourceNode.startPosition
            }

            val originalName = when (targetNode) {
                is JsFunction, is JsVars.JsVar, is JsParameter -> targetNode.name?.toString()
                else -> null
            }

            val jsLocation = JsLocation(fileName, location.line, location.offset, originalName)

            when (targetNode) {
                is SourceInfoAwareJsNode ->
                    targetNode.source = jsLocation
                is JsExpressionStatement if targetNode.expression.source == null ->
                    targetNode.expression.source = jsLocation
            }
        }
}