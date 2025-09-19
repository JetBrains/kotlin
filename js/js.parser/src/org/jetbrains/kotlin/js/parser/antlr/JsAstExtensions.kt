/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.parser.antlr

import com.google.gwt.dev.js.rhino.CodePosition
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.Token
import org.antlr.v4.runtime.tree.TerminalNode
import org.jetbrains.kotlin.js.backend.ast.JsDoubleLiteral
import org.jetbrains.kotlin.js.backend.ast.JsExpressionStatement
import org.jetbrains.kotlin.js.backend.ast.JsFunction
import org.jetbrains.kotlin.js.backend.ast.JsIntLiteral
import org.jetbrains.kotlin.js.backend.ast.JsLocation
import org.jetbrains.kotlin.js.backend.ast.JsNode
import org.jetbrains.kotlin.js.backend.ast.JsNumberLiteral
import org.jetbrains.kotlin.js.backend.ast.JsParameter
import org.jetbrains.kotlin.js.backend.ast.JsStringLiteral
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

internal val TerminalNode.startPosition: CodePosition
    get() = symbol.codePosition

internal val ParserRuleContext.stopPosition: CodePosition
    get() = stop.codePosition

internal val Token.codePosition: CodePosition
    get() = CodePosition(line, charPositionInLine)

internal fun unwrapStringLiteral(literalValue: String): String {
    if (literalValue.startsWith("'") && literalValue.endsWith("'"))
        return literalValue.removeSurrounding("'")

    if (literalValue.startsWith("\"") && literalValue.endsWith("\""))
        return literalValue.removeSurrounding("\"")

    return literalValue
}

internal fun String.toStringLiteral(): JsStringLiteral {
    return JsStringLiteral(unwrapStringLiteral(this))
}

internal fun String.toDecimalLiteral(): JsNumberLiteral {
    val intValue = toIntOrNull()
    if (intValue != null)
        return JsIntLiteral(intValue)

    return JsDoubleLiteral(toDouble())
}

internal fun String.toHexLiteral(): JsIntLiteral {
    return JsIntLiteral(removePrefix("0x").removePrefix("0X").hexToInt())
}

internal fun String.toOctalLiteral(): JsIntLiteral {
    return JsIntLiteral(removePrefix("0").toInt())
}