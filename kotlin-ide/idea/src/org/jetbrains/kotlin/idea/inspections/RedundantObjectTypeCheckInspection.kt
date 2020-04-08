/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils

class RedundantObjectTypeCheckInspection : AbstractKotlinInspection() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : KtVisitorVoid() {

            override fun visitIsExpression(expression: KtIsExpression) {
                super.visitIsExpression(expression)
                val typeReference = expression.typeReference ?: return
                if (!typeReference.isObject()) return
                holder.registerProblem(
                    expression.operationReference,
                    TextRange(0, if (expression.isNegated) 3 else 2),
                    KotlinBundle.message("redundant.type.checks.for.object"),
                    ReplaceWithEqualityFix(expression.isNegated)
                )
            }

        }
    }
}

private fun KtTypeReference.isObject(): Boolean {
    val descriptor = this.analyze()[BindingContext.TYPE, this]?.constructor?.declarationDescriptor as? ClassDescriptor
    return DescriptorUtils.isObject(descriptor)
}

private class ReplaceWithEqualityFix(isNegated: Boolean) : LocalQuickFix {
    private val isOperator = if (isNegated) "!is" else "is"

    private val equality = if (isNegated) "!==" else "==="

    override fun getName() = KotlinBundle.message("replace.with.equality.fix.text", isOperator, equality)

    override fun getFamilyName() = name

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val element = descriptor.psiElement.getParentOfType<KtIsExpression>(strict = false) ?: return
        val typeReference = element.typeReference ?: return
        val factory = KtPsiFactory(project)
        val newElement = factory.createExpressionByPattern("$0 $1 $2", element.leftHandSide, equality, typeReference.text)
        element.replace(newElement)
    }
}
