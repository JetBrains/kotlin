/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.j2k.conversions

import org.jetbrains.kotlin.j2k.ConversionContext
import org.jetbrains.kotlin.j2k.tree.*
import org.jetbrains.kotlin.j2k.tree.impl.JKFieldAccessExpressionImpl
import org.jetbrains.kotlin.j2k.tree.impl.JKKtCallExpressionImpl


class StringMethodsConversion(private val context: ConversionContext) : RecursiveApplicableConversionBase() {
    override fun applyToElement(element: JKTreeElement): JKTreeElement {
        if (element !is JKQualifiedExpression) return recurse(element)
        val receiverType = element.receiver.type(context) ?: return recurse(element)
        if (!receiverType.isStringType()) return recurse(element)

        convertLengthCall(element.selector)?.also {
            element.selector = it
        }

        convertCharAtCall(element.selector)?.also {
            element.selector = it
        }

        return recurse(element)
    }

    private fun convertLengthCall(selector: JKExpression): JKFieldAccessExpressionImpl? {
        if (selector !is JKMethodCallExpression) return null
        return if (selector.identifier.name == "length") {
            JKFieldAccessExpressionImpl(context.symbolProvider.provideByFqName("kotlin.String.length"))
        } else null
    }

    private fun convertCharAtCall(selector: JKExpression): JKExpression? {
        if (selector !is JKMethodCallExpression) return null
        return if (selector.identifier.name == "charAt") {
            JKKtCallExpressionImpl(
                context.symbolProvider.provideByFqName("kotlin.String.get"),
                selector::arguments.detached()
            )
        } else null
    }
}