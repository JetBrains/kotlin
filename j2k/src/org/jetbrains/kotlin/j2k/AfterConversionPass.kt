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

package org.jetbrains.kotlin.j2k

import org.jetbrains.kotlin.psi.JetPsiFactory
import com.intellij.openapi.project.Project
import org.jetbrains.jet.lang.diagnostics.Diagnostic
import org.jetbrains.jet.lang.diagnostics.Errors
import org.jetbrains.kotlin.psi.JetSimpleNameExpression
import org.jetbrains.kotlin.psi.JetUnaryExpression
import org.jetbrains.kotlin.psi.JetProperty

class AfterConversionPass(val project: Project, val postProcessor: PostProcessor) {
    public fun run(kotlinCode: String): String {
        val kotlinFile = JetPsiFactory(project).createAnalyzableFile(
                "fileForAfterConversionPass.kt", kotlinCode, postProcessor.contextToAnalyzeIn
        )
        val bindingContext = postProcessor.analyzeFile(kotlinFile)

        val fixes = bindingContext.getDiagnostics().map {
            val fix = fixForProblem(it)
            if (fix != null) it.getPsiElement() to fix else null
        }.filterNotNull()

        for ((psiElement, fix) in fixes) {
            if (psiElement.isValid()) {
                fix()
            }
        }

        postProcessor.doAdditionalProcessing(kotlinFile)

        return kotlinFile.getText()!!
    }

    private fun fixForProblem(problem: Diagnostic): (() -> Unit)? {
        val psiElement = problem.getPsiElement()
        return when (problem.getFactory()) {
            Errors.UNNECESSARY_NOT_NULL_ASSERTION -> { () ->
                val exclExclOp = psiElement as JetSimpleNameExpression
                val exclExclExpr = exclExclOp.getParent() as JetUnaryExpression
                exclExclExpr.replace(exclExclExpr.getBaseExpression()!!)
            }

            Errors.VAL_REASSIGNMENT -> { () ->
                val property = (psiElement as? JetSimpleNameExpression)?.getReference()?.resolve() as? JetProperty
                if (property != null && !property.isVar()) {
                    property.getValOrVarNode().getPsi()!!.replace(JetPsiFactory(project).createVarNode().getPsi()!!)
                }
            }

            else -> null
        }
    }
}
