/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.k2js.translate.expression

import com.google.dart.compiler.backend.js.ast.*
import org.jetbrains.jet.lang.psi.JetTryExpression
import org.jetbrains.jet.lang.psi.JetExpression
import org.jetbrains.jet.lang.psi.JetCatchClause
import org.jetbrains.k2js.translate.context.TranslationContext
import org.jetbrains.k2js.translate.general.AbstractTranslator
import org.jetbrains.k2js.translate.general.Translation.translateAsStatementAndMergeInBlockIfNeeded
import org.jetbrains.k2js.translate.utils.JsAstUtils.convertToBlock

public class TryTranslator(
        val expression: JetTryExpression,
        context: TranslationContext
) : AbstractTranslator(context) {
    public fun translate(): JsTry {
        val tryBlock = translateAsBlock(expression.getTryBlock())

        val catchTranslator = CatchTranslator(expression.getCatchClauses(), context())
        val catchBlock = catchTranslator.translate()

        val finallyExpression = expression.getFinallyBlock()?.getFinalExpression()
        val finallyBlock = translateAsBlock(finallyExpression)

        return JsTry(tryBlock, catchBlock, finallyBlock)
    }

    private fun translateAsBlock(expression: JetExpression?): JsBlock? {
        if (expression == null) return null

        val statement = translateAsStatementAndMergeInBlockIfNeeded(expression, context())
        return convertToBlock(statement)
    }
}

