/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.conversions

import org.jetbrains.kotlin.nj2k.tree.*
import org.jetbrains.kotlin.nj2k.tree.impl.JKKtLiteralExpressionImpl
import java.math.BigInteger

class LiteralConversion : RecursiveApplicableConversionBase() {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element !is JKJavaLiteralExpression) return recurse(element)
        return element.convertLiteral()
    }

    private fun JKLiteralExpression.convertLiteral(): JKLiteralExpression =
        when (type) {
            JKLiteralExpression.LiteralType.DOUBLE -> toDoubleLiteral()
            JKLiteralExpression.LiteralType.FLOAT -> toFloatLiteral()
            JKLiteralExpression.LiteralType.LONG -> toLongLiteral()
            JKLiteralExpression.LiteralType.INT -> toIntLiteral()
            JKLiteralExpression.LiteralType.CHAR -> convertCharLiteral()
            JKLiteralExpression.LiteralType.STRING -> toStringLiteral()
            else -> this
        }.withNonCodeElementsFrom(this)

    private fun JKLiteralExpression.toDoubleLiteral() =
        JKKtLiteralExpressionImpl(
            literal.cleanFloatAndDoubleLiterals()
                .let { text ->
                    if (!text.contains(".") && !text.contains("e", true))
                        "$text."
                    else text
                }.let { text ->
                    if (text.endsWith(".")) "${text}0" else text
                },
            JKLiteralExpression.LiteralType.DOUBLE
        )


    private fun JKLiteralExpression.toFloatLiteral() =
        JKKtLiteralExpressionImpl(
            literal.cleanFloatAndDoubleLiterals()
                .let { text ->
                    if (!text.endsWith("f")) "${text}f"
                    else text
                },
            JKLiteralExpression.LiteralType.FLOAT
        )

    private fun JKLiteralExpression.toStringLiteral() =
        JKKtLiteralExpressionImpl(literal.replace("((?:\\\\)*)\\\\([0-3]?[0-7]{1,2})".toRegex()) { matchResult ->
            val leadingBackslashes = matchResult.groupValues[1]
            if (leadingBackslashes.length % 2 == 0)
                String.format("%s\\u%04x", leadingBackslashes, Integer.parseInt(matchResult.groupValues[2], 8))
            else matchResult.value
        }.replace("\\$([A-Za-z]+|\\{)".toRegex(), "\\\\$0"), JKLiteralExpression.LiteralType.STRING)


    private fun JKLiteralExpression.convertCharLiteral(): JKKtLiteralExpression =
        JKKtLiteralExpressionImpl(
            literal.replace("\\\\([0-3]?[0-7]{1,2})".toRegex()) {
                String.format("\\u%04x", Integer.parseInt(it.groupValues[1], 8))
            },
            JKLiteralExpression.LiteralType.CHAR
        )


    private fun JKLiteralExpression.toIntLiteral(): JKKtLiteralExpression =
        JKKtLiteralExpressionImpl(
            literal
                .cleanIntAndLongLiterals()
                .convertHexLiteral(isLongLiteral = false)
                .convertBinaryLiteral(isLongLiteral = false)
                .convertOctalLiteral(isLongLiteral = false),
            JKLiteralExpression.LiteralType.INT
        )


    private fun JKLiteralExpression.toLongLiteral(): JKKtLiteralExpression =
        JKKtLiteralExpressionImpl(
            literal
                .cleanIntAndLongLiterals()
                .convertHexLiteral(isLongLiteral = true)
                .convertBinaryLiteral(isLongLiteral = true)
                .convertOctalLiteral(isLongLiteral = true) + "L",
            JKLiteralExpression.LiteralType.LONG
        )

    private fun String.convertHexLiteral(isLongLiteral: Boolean): String {
        if (!startsWith("0x", ignoreCase = true)) return this
        val value = BigInteger(drop(2), 16)
        return when {
            isLongLiteral && value.bitLength() > 63 ->
                "-0x${value.toLong().toString(16).substring(1)}"

            !isLongLiteral && value.bitLength() > 31 ->
                "-0x${value.toInt().toString(16).substring(1)}"

            else -> this
        }
    }

    private fun String.convertBinaryLiteral(isLongLiteral: Boolean): String {
        if (!startsWith("0b", ignoreCase = true)) return this
        val value = BigInteger(drop(2), 2)
        return if (isLongLiteral) value.toLong().toString(10) else value.toInt().toString()
    }

    private fun String.convertOctalLiteral(isLongLiteral: Boolean): String {
        if (!startsWith("0") || length == 1 || get(1).toLowerCase() == 'x') return this
        val value = BigInteger(drop(1), 8)
        return if (isLongLiteral) value.toLong().toString(10) else value.toInt().toString(10)
    }

    private fun String.cleanFloatAndDoubleLiterals() =
        replace("L", "", ignoreCase = true)
            .replace("d", "", ignoreCase = true)
            .replace(".e", "e", ignoreCase = true)
            .replace(".f", "", ignoreCase = true)
            .replace("f", "", ignoreCase = true)
            .replace("_", "")

    private fun String.cleanIntAndLongLiterals() =
        replace("l", "", ignoreCase = true)
            .replace("_", "")
}
