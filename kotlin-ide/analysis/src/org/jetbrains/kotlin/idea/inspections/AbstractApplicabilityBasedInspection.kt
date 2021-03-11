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

import com.intellij.codeInspection.*
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtVisitorVoid

abstract class AbstractApplicabilityBasedInspection<TElement : KtElement>(
    val elementType: Class<TElement>
) : AbstractKotlinInspection() {

    final override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession) =
        object : KtVisitorVoid() {
            override fun visitKtElement(element: KtElement) {
                super.visitKtElement(element)

                if (!elementType.isInstance(element) || element.textLength == 0) return
                @Suppress("UNCHECKED_CAST")
                visitTargetElement(element as TElement, holder, isOnTheFly)
            }
        }

    // This function should be called from visitor built by a derived inspection
    protected fun visitTargetElement(element: TElement, holder: ProblemsHolder, isOnTheFly: Boolean) {
        if (!isApplicable(element)) return

        holder.registerProblemWithoutOfflineInformation(
            element,
            inspectionText(element),
            isOnTheFly,
            inspectionHighlightType(element),
            inspectionHighlightRangeInElement(element),
            LocalFix(fixText(element))
        )
    }

    open fun inspectionHighlightRangeInElement(element: TElement): TextRange? = null

    open fun inspectionHighlightType(element: TElement): ProblemHighlightType = ProblemHighlightType.GENERIC_ERROR_OR_WARNING

    abstract fun inspectionText(element: TElement): String

    abstract val defaultFixText: String

    open fun fixText(element: TElement) = defaultFixText

    abstract fun isApplicable(element: TElement): Boolean

    abstract fun applyTo(element: TElement, project: Project = element.project, editor: Editor? = null)

    open val startFixInWriteAction = true

    private inner class LocalFix(val text: String) : LocalQuickFix {
        override fun startInWriteAction() = startFixInWriteAction

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            @Suppress("UNCHECKED_CAST")
            val element = descriptor.psiElement as TElement
            applyTo(element, project, element.findExistingEditor())
        }

        override fun getFamilyName() = defaultFixText

        override fun getName() = text
    }
}