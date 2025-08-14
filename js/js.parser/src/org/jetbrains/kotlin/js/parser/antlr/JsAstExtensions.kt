/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.parser.antlr

import com.google.gwt.dev.js.rhino.CodePosition
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.Token
import org.jetbrains.kotlin.js.backend.ast.JsExpressionStatement
import org.jetbrains.kotlin.js.backend.ast.JsFunction
import org.jetbrains.kotlin.js.backend.ast.JsLocation
import org.jetbrains.kotlin.js.backend.ast.JsNode
import org.jetbrains.kotlin.js.backend.ast.JsParameter
import org.jetbrains.kotlin.js.backend.ast.JsVars
import org.jetbrains.kotlin.js.backend.ast.SourceInfoAwareJsNode
import org.jetbrains.kotlin.js.parser.antlr.generated.JavaScriptParser

internal fun ParserRuleContext.toJsLocationRelativeTo(file: String): JsLocation {
    return JsLocation(file, start.line, start.charPositionInLine)
}

internal fun ParserRuleContext.toJsLocationRelativeTo(origin: JsLocation): JsLocation {
    return JsLocation(origin.file, origin.startLine + start.line, origin.startChar + start.charPositionInLine)
}

internal val ParserRuleContext.startPosition: CodePosition
    get() = start.codePosition

internal val ParserRuleContext.stopPosition: CodePosition
    get() = stop.codePosition

internal val Token.codePosition: CodePosition
    get() = CodePosition(line, charPositionInLine)

internal fun <T : JsNode> T.applyLocation(fileName: String, sourceNode: ParserRuleContext): T =
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