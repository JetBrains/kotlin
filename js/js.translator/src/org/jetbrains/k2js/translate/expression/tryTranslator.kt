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

package org.jetbrains.k2js.translate.expression.tryTranslator

import com.google.dart.compiler.backend.js.ast.*
import org.jetbrains.jet.lang.psi.JetCatchClause
import org.jetbrains.jet.lang.psi.JetTryExpression
import org.jetbrains.jet.lang.psi.JetTypeReference
import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.jet.lang.resolve.BindingContextUtils.getNotNull
import org.jetbrains.jet.lang.resolve.DescriptorUtils
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns
import org.jetbrains.k2js.translate.context.TranslationContext
import org.jetbrains.k2js.translate.general.Translation
import org.jetbrains.k2js.translate.general.Translation.translateAsStatementAndMergeInBlockIfNeeded
import org.jetbrains.k2js.translate.utils.JsAstUtils
import org.jetbrains.k2js.translate.utils.JsAstUtils.convertToBlock
import org.jetbrains.k2js.translate.utils.TranslationUtils


val THROWABLE_CLASS_NAME = DescriptorUtils.getFqNameSafe(KotlinBuiltIns.getInstance().getThrowable()).asString()

val JetCatchClause.parameterTypeReference: JetTypeReference
    get() = this.getCatchParameter()?.getTypeReference()!!

public fun translateTryExpression(expression: JetTryExpression, context: TranslationContext): JsTry {

    fun translateTryBlock() = convertToBlock(translateAsStatementAndMergeInBlockIfNeeded(expression.getTryBlock(), context))

    fun JetCatchClause.getParameterName(): JsName = context.getNameForElement(this.getCatchParameter()!!)

    fun JetTypeReference.isThrowable(): Boolean {
        val jetType = getNotNull(context.bindingContext(), BindingContext.TYPE, this)
        return THROWABLE_CLASS_NAME == TranslationUtils.getJetTypeFqName(jetType, false)
    }

    fun translateCatchBody(catchClause: JetCatchClause): JsBlock {
        val catchBody = catchClause.getCatchBody()
        if (catchBody == null) {
            return convertToBlock(context.getEmptyStatement())
        }
        return convertToBlock(translateAsStatementAndMergeInBlockIfNeeded(catchBody, context))
    }

    fun translateCatches(): JsCatch? {
        val patternTranslator = Translation.patternTranslator(context)
        val catchClausesStream = expression.getCatchClauses().stream()
        if (catchClausesStream.none()) return null

        val firstCatchClause = catchClausesStream.first()
        val firstParameterRef = JsNameRef(firstCatchClause.getParameterName())

        fun jsCatch(body: JsBlock): JsCatch {
            val result = JsCatch(context.scope(), firstParameterRef.getIdent())
            result.setBody(body)
            return result
        }

        fun ifIsCheckThen(typeReference: JetTypeReference, catchBlock: JsBlock): JsIf =
                JsIf(patternTranslator.translateIsCheck(firstParameterRef, typeReference), catchBlock)

        if (firstCatchClause.parameterTypeReference.isThrowable()) {
            return jsCatch(translateCatchBody(firstCatchClause))
        }

        val resultIf = ifIsCheckThen(firstCatchClause.parameterTypeReference, translateCatchBody(firstCatchClause))
        val currentIf = catchClausesStream.drop(1).fold(resultIf) { (currentIf, catchClause) ->
            val typeReference = catchClause.parameterTypeReference
            val catchBlock = translateCatchBody(catchClause)
            if (typeReference.isThrowable()) {
                currentIf.setElseStatement(catchBlock)
                currentIf
            }
            else {
                val nextIf = ifIsCheckThen(typeReference, catchBlock)
                catchBlock.getStatements().add(0, JsAstUtils.newVar(catchClause.getParameterName(), firstParameterRef))
                currentIf.setElseStatement(nextIf)
                nextIf
            }
        }

        if (currentIf.getElseStatement() == null) {
            currentIf.setElseStatement(JsThrow(firstParameterRef))
        }

        return jsCatch(JsAstUtils.convertToBlock(resultIf))
    }

    fun translateFinallyBlock(): JsBlock? {
        val finalExpression = expression.getFinallyBlock()?.getFinalExpression()
        if (finalExpression == null) return  null

        return convertToBlock(translateAsStatementAndMergeInBlockIfNeeded(finalExpression, context))
    }

    return JsTry(translateTryBlock(), translateCatches(), translateFinallyBlock())
}
