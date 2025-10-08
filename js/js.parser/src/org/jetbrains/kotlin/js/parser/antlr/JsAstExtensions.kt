/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.parser.antlr

import org.jetbrains.kotlin.js.parser.CodePosition
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.Token
import org.antlr.v4.runtime.tree.TerminalNode
import org.jetbrains.kotlin.js.backend.ast.JsDoubleLiteral
import org.jetbrains.kotlin.js.backend.ast.JsIntLiteral
import org.jetbrains.kotlin.js.backend.ast.JsNumberLiteral
import org.jetbrains.kotlin.js.backend.ast.JsStringLiteral

internal val ParserRuleContext.startPosition: CodePosition
    get() = start.startPosition

internal val TerminalNode.startPosition: CodePosition
    get() = symbol.startPosition

internal val ParserRuleContext.stopPosition: CodePosition
    get() = stop.stopPosition

internal val TerminalNode.stopPosition: CodePosition
    get() = symbol.stopPosition

// JS AST line positioning is 0-based, while ANTLR line positioning is 1-based, so there is a need to adjust it
internal val Token.startPosition: CodePosition
    get() = CodePosition(line - 1, charPositionInLine)

/**
 * ANTLR doesn't provide a token's end position, so we calculate it based on the token string value and its line positioning.
 * Use it only for informational purposes, like warnings or inspections, but not for precise calculations.
 */
internal val Token.stopPosition: CodePosition
    get() {
        val text = text ?: ""
        if (text.contains('\n')) {
            val lines = text.split('\n')
            val endLine = startPosition.line + lines.size - 1
            val endColumn = lines.last().length
            return CodePosition(endLine, endColumn)
        } else {
            return CodePosition(startPosition.line, startPosition.offset + text.length)
        }
    }

internal fun unwrapStringLiteral(literalValue: String): String {
    literalValue.run {
        if (startsWith("'") && endsWith("'"))
            return removeSurrounding("'")

        if (startsWith("\"") && endsWith("\""))
            return removeSurrounding("\"")

        return this
    }
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

internal fun String.toHexLiteral(): JsNumberLiteral {
    val cleanHex = removePrefix("0x").removePrefix("0X")
    val longValue = cleanHex.toLong(16)

    return if (longValue in Int.MIN_VALUE..Int.MAX_VALUE)
        JsIntLiteral(longValue.toInt())
    else
        JsDoubleLiteral(longValue.toDouble())
}

internal fun String.toOctalLiteral(): JsNumberLiteral {
    val longValue = removePrefix("0").toLong(8)

    return if (longValue in Int.MIN_VALUE..Int.MAX_VALUE)
        JsIntLiteral(longValue.toInt())
    else
        JsDoubleLiteral(longValue.toDouble())
}

internal fun String.unescapeUnicode(): String {
    val chars = this.toCharArray()

    return buildString(this.length) {
        var i = 0

        while (i < chars.size) {
            if (chars[i] == '\\' && i + 1 < chars.size && chars[i + 1] == 'u' && i + 5 < chars.size) {
                val hex = String(chars, i + 2, 4)
                append(hex.toInt(16).toChar())
                i += 6
            } else {
                append(chars[i])
                i++
            }
        }
    }
}