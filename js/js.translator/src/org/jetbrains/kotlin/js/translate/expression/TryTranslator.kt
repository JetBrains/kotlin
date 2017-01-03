/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.js.translate.expression

import org.jetbrains.kotlin.js.backend.ast.JsBlock
import org.jetbrains.kotlin.js.backend.ast.JsTry
import org.jetbrains.kotlin.psi.KtTryExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.js.translate.context.TranslationContext
import org.jetbrains.kotlin.js.translate.general.AbstractTranslator
import org.jetbrains.kotlin.js.translate.general.Translation.translateAsStatementAndMergeInBlockIfNeeded
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils.convertToBlock

class TryTranslator(
        val expression: KtTryExpression,
        context: TranslationContext
) : AbstractTranslator(context) {
    fun translate(): JsTry {
        val tryBlock = translateAsBlock(expression.tryBlock)

        val catchTranslator = CatchTranslator(expression.catchClauses, context())
        val catchBlock = catchTranslator.translate()

        val finallyExpression = expression.finallyBlock?.finalExpression
        val finallyBlock = translateAsBlock(finallyExpression)

        return JsTry(tryBlock, catchBlock, finallyBlock)
    }

    private fun translateAsBlock(expression: KtExpression?): JsBlock? {
        if (expression == null) return null

        val statement = translateAsStatementAndMergeInBlockIfNeeded(expression, context())
        return convertToBlock(statement)
    }
}

