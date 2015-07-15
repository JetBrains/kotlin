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

public class MethodIntoObjectProcessing(private val method: PsiMethod, private val objectName: String) : UsageProcessing {
    override val targetElement: PsiElement get() = method

    override val convertedCodeProcessor: ConvertedCodeProcessor? get() = null

    override val javaCodeProcessor = object: ExternalCodeProcessor {
        override fun processUsage(reference: PsiReference): Collection<PsiReference>? {
            val refExpr = reference.getElement() as? PsiReferenceExpression ?: return null
            val qualifier = refExpr.getQualifierExpression()
            val factory = PsiElementFactory.SERVICE.getInstance(method.getProject())
            if (qualifier != null) {
                val newQualifier = factory.createExpressionFromText(qualifier.getText() + "." + objectName, null)
                qualifier.replace(newQualifier)
                return listOf(reference)
            }
            else {
                var qualifiedExpr = factory.createExpressionFromText(objectName + "." + refExpr.getText(), null) as PsiReferenceExpression
                qualifiedExpr = refExpr.replace(qualifiedExpr) as PsiReferenceExpression
                return listOf(qualifiedExpr.getReference()!!)
            }
        }
    }

    override val kotlinCodeProcessor: ExternalCodeProcessor? get() = null
}