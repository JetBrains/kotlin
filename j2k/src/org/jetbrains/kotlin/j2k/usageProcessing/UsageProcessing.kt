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

package org.jetbrains.kotlin.j2k.usageProcessing

import com.intellij.psi.*
import org.jetbrains.kotlin.j2k.CodeConverter
import org.jetbrains.kotlin.j2k.SpecialExpressionConverter
import org.jetbrains.kotlin.j2k.ast.Expression

interface UsageProcessing {
    val targetElement: PsiElement
    val convertedCodeProcessor: ConvertedCodeProcessor?
    val javaCodeProcessors: List<ExternalCodeProcessor>
    val kotlinCodeProcessors: List<ExternalCodeProcessor>
}

interface ConvertedCodeProcessor {
    fun convertVariableUsage(expression: PsiReferenceExpression, codeConverter: CodeConverter): Expression? = null

    fun convertMethodUsage(methodCall: PsiMethodCallExpression, codeConverter: CodeConverter): Expression? = null
}

interface ExternalCodeProcessor {
    fun processUsage(reference: PsiReference): Array<PsiReference>?
}

class UsageProcessingExpressionConverter(val processings: Map<PsiElement, Collection<UsageProcessing>>) : SpecialExpressionConverter {
    override fun convertExpression(expression: PsiExpression, codeConverter: CodeConverter): Expression? {
        if (processings.isEmpty()) return null

        when (expression) {
            is PsiReferenceExpression -> {
                val target = expression.resolve() as? PsiVariable ?: return null
                val forTarget = processings[target] ?: return null
                for (processing in forTarget) {
                    val converted = processing.convertedCodeProcessor?.convertVariableUsage(expression, codeConverter)
                    if (converted != null) return converted
                }
                return null
            }

            is PsiMethodCallExpression -> {
                val target = expression.methodExpression.resolve() as? PsiMethod ?: return null
                val forTarget = processings[target] ?: return null
                for (processing in forTarget) {
                    val converted = processing.convertedCodeProcessor?.convertMethodUsage(expression, codeConverter)
                    if (converted != null) return converted
                }
                return null
            }

            else -> return null
        }
    }
}
