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
import org.jetbrains.jet.lang.psi.JetCatchClause
import org.jetbrains.jet.lang.psi.JetExpression
import org.jetbrains.jet.lang.psi.JetTypeReference
import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.jet.lang.resolve.BindingContextUtils.getNotNull
import org.jetbrains.jet.lang.resolve.DescriptorUtils
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns
import org.jetbrains.k2js.translate.context.TranslationContext
import org.jetbrains.k2js.translate.general.AbstractTranslator
import org.jetbrains.k2js.translate.general.Translation.patternTranslator
import org.jetbrains.k2js.translate.general.Translation.translateAsStatementAndMergeInBlockIfNeeded
import org.jetbrains.k2js.translate.utils.JsAstUtils.convertToBlock
import org.jetbrains.k2js.translate.utils.TranslationUtils
import java.util.Collections
import org.jetbrains.k2js.translate.utils.JsAstUtils

class CatchTranslator(
        val catches: List<JetCatchClause>,
        context: TranslationContext
) : AbstractTranslator(context) {

    /**
     * In JavaScript there is no multiple catches, so we translate
     * multiple catch to single catch with instanceof checks for
     * every catch clause.
     *
     * For example this code:
     *  try {
     *      ...
     *  } catch(e: NullPointerException) {
     *      ...
     *  } catch(e: RuntimeException) {
     *      ...
     *  }
     *
     *  is translated to the following JsCode
     *
     *  try {
     *      ...
     *  } catch(e) {
     *      if (e instanceof NullPointerException) {
     *          ...
     *      } else {
     *          if (e instanceof RuntimeException) {
     *              ...
     *          } else throw e;
     *      }
     *  }
     */
    public fun translate(): JsCatch? {
        if (catches.empty) return null

        val firstCatch = catches.first!!
        val catchParameter = firstCatch.getCatchParameter()
        val parameterName = context().getNameForElement(catchParameter!!)
        val parameterRef = parameterName.makeRef()

        return JsCatch(context().scope(),
                       parameterRef.getIdent(),
                       translateCatches(parameterRef, catches.iterator()))
    }

    private fun translateCatches(parameterRef: JsNameRef, catches: Iterator<JetCatchClause>): JsStatement {
        if (!catches.hasNext()) return JsThrow(parameterRef)

        val catch = catches.next()
        val param = catch.getCatchParameter()!!
        val paramName = context().getNameForElement(param)
        val paramType = param.getTypeReference()!!

        val thenBlock = translateCatchBody(context(), catch)
        if (paramName.getIdent() != parameterRef.getIdent())
            thenBlock.getStatements().add(0, JsAstUtils.newVar(paramName, parameterRef))

        if (paramType.isThrowable) return thenBlock

        val typeCheck = with (patternTranslator(context())) {
            translateIsCheck(parameterRef, paramType)
        }

        val elseBlock = translateCatches(parameterRef, catches)
        return JsIf(typeCheck, thenBlock, elseBlock)
    }

    private fun translateCatchBody(context: TranslationContext, catchClause: JetCatchClause): JsBlock {
        val catchBody = catchClause.getCatchBody()
        val jsCatchBody =
                if (catchBody != null)
                    translateAsStatementAndMergeInBlockIfNeeded(catchBody, context)
                else
                    context.getEmptyExpression().makeStmt()

        return convertToBlock(jsCatchBody)
    }

    private val JetTypeReference.isThrowable: Boolean
        get() {
            val jetType = getNotNull(bindingContext(), BindingContext.TYPE, this)
            val jetTypeName = TranslationUtils.getJetTypeFqName(jetType, false)

            val throwable = KotlinBuiltIns.getInstance().getThrowable()
            val throwableClassName = DescriptorUtils.getFqNameSafe(throwable).asString()

            return jetTypeName == throwableClassName
        }
}