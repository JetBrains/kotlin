/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.uast.kotlin

import org.jetbrains.kotlin.psi.*
import org.jetbrains.uast.*

interface BaseKotlinConverter {

    fun convertReceiverParameter(receiver: KtTypeReference): UParameter? {
        val call = (receiver.parent as? KtCallableDeclaration) ?: return null
        if (call.receiverTypeReference != receiver) return null
        return call.toUElementOfType<UMethod>()?.uastParameters?.firstOrNull()
    }

    fun convertExpression(
        expression: KtExpression,
        givenParent: UElement?,
        requiredTypes: Array<out Class<out UElement>>
    ): UExpression?

    fun convertEntry(
        entry: KtStringTemplateEntry,
        givenParent: UElement?,
        requiredTypes: Array<out Class<out UElement>>
    ): UExpression? {
        return with(requiredTypes) {
            if (entry is KtStringTemplateEntryWithExpression) {
                expr<UExpression> {
                    convertOrEmpty(entry.expression, givenParent)
                }
            } else {
                expr<ULiteralExpression> {
                    if (entry is KtEscapeStringTemplateEntry)
                        KotlinStringULiteralExpression(entry, givenParent, entry.unescapedValue)
                    else
                        KotlinStringULiteralExpression(entry, givenParent)
                }
            }
        }
    }

    fun convertOrEmpty(expression: KtExpression?, parent: UElement?): UExpression {
        return expression?.let { convertExpression(it, parent, DEFAULT_EXPRESSION_TYPES_LIST) } ?: UastEmptyExpression(parent)
    }

    fun convertOrNull(expression: KtExpression?, parent: UElement?): UExpression? {
        return if (expression != null) convertExpression(expression, parent, DEFAULT_EXPRESSION_TYPES_LIST) else null
    }
}
