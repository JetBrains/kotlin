/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.util

import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*

/**
 * This extension method was copied from `idea:idea-frontend-independent` module because it's the only method that
 * included in `idea:idea-frontend-independent` and needed for performing REPL completion
 */
@Suppress("unused")
fun KtFunctionLiteral.findLabelAndCall(): Pair<Name?, KtCallExpression?> {
    val literalParent = (this.parent as KtLambdaExpression).parent

    fun KtValueArgument.callExpression(): KtCallExpression? {
        val parent = parent
        return (if (parent is KtValueArgumentList) parent else this).parent as? KtCallExpression
    }

    when (literalParent) {
        is KtLabeledExpression -> {
            val callExpression = (literalParent.parent as? KtValueArgument)?.callExpression()
            return Pair(literalParent.getLabelNameAsName(), callExpression)
        }

        is KtValueArgument -> {
            val callExpression = literalParent.callExpression()
            val label = (callExpression?.calleeExpression as? KtSimpleNameExpression)?.getReferencedNameAsName()
            return Pair(label, callExpression)
        }

        else -> {
            return Pair(null, null)
        }
    }
}
