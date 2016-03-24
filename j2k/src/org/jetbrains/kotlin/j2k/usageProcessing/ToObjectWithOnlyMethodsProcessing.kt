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
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.utils.addToStdlib.singletonList

class ToObjectWithOnlyMethodsProcessing(private val psiClass: PsiClass) : UsageProcessing {
    override val targetElement: PsiElement get() = psiClass

    override val convertedCodeProcessor: ConvertedCodeProcessor? get() = null

    override val javaCodeProcessors = ToObjectWithOnlyMethodsProcessor().singletonList()

    override val kotlinCodeProcessors = emptyList<ExternalCodeProcessor>()

    inner class ToObjectWithOnlyMethodsProcessor: ExternalCodeProcessor {
        override fun processUsage(reference: PsiReference): Array<PsiReference>? {
            val refExpr = reference.element as? PsiReferenceExpression ?: return null
            val factory = PsiElementFactory.SERVICE.getInstance(psiClass.project)
            var qualifiedExpr = factory.createExpressionFromText(refExpr.text + "." + JvmAbi.INSTANCE_FIELD, null) as PsiReferenceExpression
            qualifiedExpr = refExpr.replace(qualifiedExpr) as PsiReferenceExpression
            return arrayOf(qualifiedExpr)
        }
    }
}