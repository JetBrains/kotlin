/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.j2k.conversions

import org.jetbrains.kotlin.j2k.tree.JKJavaLiteralExpression
import org.jetbrains.kotlin.j2k.tree.JKKtLiteralExpression
import org.jetbrains.kotlin.j2k.tree.JKLiteralExpression.LiteralType.*
import org.jetbrains.kotlin.j2k.tree.JKTreeElement
import org.jetbrains.kotlin.j2k.tree.impl.JKKtLiteralExpressionImpl
import java.math.BigInteger

class LiteralConversion : RecursiveApplicableConversionBase() {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element !is JKJavaLiteralExpression) return recurse(element)

        return when {
            element.type == DOUBLE -> convertDoubleLiteral(element.literal)
            element.type == FLOAT -> convertFloatLiteral(element.literal)
            element.type == LONG || element.type == INT -> convertIntegerLiteral(element)
            element.type == CHAR -> convertCharLiteral(element.literal)
            element.type == STRING -> convertStringLiteral(element.literal)
            else -> element
        }
    }

    private fun convertDoubleLiteral(text: String): JKKtLiteralExpression {
        var newText =
            text
                .replace("d", "", true)
                .replace(".e", "e", true)
                .replace(".f", "", true)
                .replace("f", "", true)

        if (!newText.contains(".") && !newText.contains("e", true))
            newText += "."
        if (newText.endsWith("."))
            newText += "0"

        return JKKtLiteralExpressionImpl(
            newText,
            DOUBLE
        )
    }

    private fun convertFloatLiteral(text: String): JKKtLiteralExpressionImpl {
        return JKKtLiteralExpressionImpl(
            text.replace(".f", "f", true)
                .replace("F", "f")
                .replace(".e", "e", true)
                .let {
                    if (!it.endsWith("f")) "${it}f"
                    else it
                },
            FLOAT
        )
    }

    private fun convertStringLiteral(text: String): JKKtLiteralExpressionImpl {
        var newText = text.replace("((?:\\\\)*)\\\\([0-3]?[0-7]{1,2})".toRegex()) {
            val leadingBackslashes = it.groupValues[1]
            if (leadingBackslashes.length % 2 == 0) {
                String.format("%s\\u%04x", leadingBackslashes, Integer.parseInt(it.groupValues[2], 8))
            } else {
                it.value
            }
        }
        newText = newText.replace("\\$([A-Za-z]+|\\{)".toRegex(), "\\\\$0")

        return JKKtLiteralExpressionImpl(newText, STRING)
    }

    private fun convertCharLiteral(text: String): JKKtLiteralExpression {
        return JKKtLiteralExpressionImpl(
            text.replace("\\\\([0-3]?[0-7]{1,2})".toRegex()) {
                String.format("\\u%04x", Integer.parseInt(it.groupValues[1], 8))
            },
            CHAR
        )
    }

    private fun convertIntegerLiteral(element: JKJavaLiteralExpression): JKKtLiteralExpression {
        var text = element.literal
        if (element.type == LONG) {
            text = text.replace("l", "L")
        }

        fun isHexLiteral(text: String) = text.startsWith("0x") || text.startsWith("0X")

        if ((element.type == LONG || element.type == INT) && isHexLiteral(text)) {
            val v = BigInteger(text.substring(2).replace("L", ""), 16)
            if (text.contains("L")) {
                if (v.bitLength() > 63) {
                    text = "-0x${v.toLong().toString(16).substring(1)}L"
                }
            } else {
                if (v.bitLength() > 31) {
                    text = "-0x${v.toInt().toString(16).substring(1)}"
                }
            }
        } else if (element.type == INT) {
            text = element.literal
        }

        return JKKtLiteralExpressionImpl(
            text,
            element.type
        )
    }
}
