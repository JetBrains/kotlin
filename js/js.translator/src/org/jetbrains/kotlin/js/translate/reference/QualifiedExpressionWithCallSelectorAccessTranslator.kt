/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.translate.reference

import org.jetbrains.kotlin.js.backend.ast.JsExpression
import org.jetbrains.kotlin.js.translate.context.TranslationContext
import org.jetbrains.kotlin.psi.KtCallExpression

class QualifiedExpressionWithCallSelectorAccessTranslator(
    private val selector: KtCallExpression,
    private val receiver: JsExpression?,
    private val context: TranslationContext
) : AccessTranslator {

    override fun translateAsGet(): JsExpression {
        return QualifiedExpressionTranslator.invokeCallExpressionTranslator(receiver, selector, context) as JsExpression
    }

    override fun translateAsSet(setTo: JsExpression): JsExpression {
        throw IllegalStateException("Set access is not supported for " + this::class.java.simpleName)
    }

    override fun getCached(): AccessTranslator {
        throw IllegalStateException("Cashed access is not supported for " + this::class.java.simpleName)
    }
}