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

package org.jetbrains.jet.j2k.usageProcessing

import org.jetbrains.jet.j2k.ast.Expression
import com.intellij.psi.*
import org.jetbrains.jet.j2k.CodeConverter
import org.jetbrains.jet.j2k.SpecialExpressionConverter

trait UsageProcessing {
    val targetElement: PsiElement
    val convertedCodeProcessor: ConvertedCodeProcessor?
    val javaCodeProcessor: ExternalCodeProcessor?
    val kotlinCodeProcessor: ExternalCodeProcessor?
}

trait ConvertedCodeProcessor {
    fun convertVariableUsage(expression: PsiReferenceExpression, codeConverter: CodeConverter): Expression? = null

    fun convertMethodUsage(methodCall: PsiMethodCallExpression, codeConverter: CodeConverter): Expression? = null
}

trait ExternalCodeProcessor {
    fun processUsage(reference: PsiReference)
}

class UsageProcessingExpressionConverter(processings: Collection<UsageProcessing>) : SpecialExpressionConverter {
    private val targetToProcessing = processings.toMap { it.targetElement } // we assume that there will be no more than one processing for one target element

    override fun convertExpression(expression: PsiExpression, codeConverter: CodeConverter): Expression? {
        if (targetToProcessing.isEmpty()) return null

        //TODO: method usages

        if (expression is PsiReferenceExpression) {
            val target = expression.resolve() ?: return null
            val processor = targetToProcessing[target]?.convertedCodeProcessor ?: return null
            return processor.convertVariableUsage(expression, codeConverter)
        }

        return null
    }
}