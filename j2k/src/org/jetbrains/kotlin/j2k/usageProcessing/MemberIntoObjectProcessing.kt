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

class MemberIntoObjectProcessing(private val member: PsiMember, private val objectName: String) : UsageProcessing {
    override val targetElement: PsiElement get() = member

    override val convertedCodeProcessor: ConvertedCodeProcessor? get() = null

    override val javaCodeProcessors = listOf(AppendObjectNameProcessor())

    override val kotlinCodeProcessors = emptyList<ExternalCodeProcessor>()

    inner class AppendObjectNameProcessor : ExternalCodeProcessor {
        override fun processUsage(reference: PsiReference): Array<PsiReference>? {
            val refExpr = reference.element as? PsiReferenceExpression ?: return null
            val qualifier = refExpr.qualifierExpression
            val factory = PsiElementFactory.SERVICE.getInstance(member.project)
            if (qualifier != null) {
                val newQualifier = factory.createExpressionFromText(qualifier.text + "." + objectName, null)
                qualifier.replace(newQualifier)
                return arrayOf(reference)
            }
            else {
                var qualifiedExpr = factory.createExpressionFromText(objectName + "." + refExpr.text, null) as PsiReferenceExpression
                qualifiedExpr = refExpr.replace(qualifiedExpr) as PsiReferenceExpression
                return arrayOf(qualifiedExpr)
            }
        }
    }
}