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
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.SmartPointerManager
import org.jetbrains.kotlin.idea.KotlinBundle

class ReplaceStringInDocumentFix(element: PsiElement, private val oldString: String, private val newString: String) : LocalQuickFix {
    private val elementRef = SmartPointerManager.getInstance(element.project).createSmartPsiElementPointer(element)

    override fun applyFix(project: Project, problemDescriptor: ProblemDescriptor) {
        val element = elementRef.element ?: return
        val virtualFile = element.containingFile?.virtualFile ?: return
        val document = FileDocumentManager.getInstance().getDocument(virtualFile) ?: return

        val text = element.text
        val index = text.indexOf(oldString)
        if (index < 0) return

        val start = element.textOffset + index
        val end = start + oldString.length
        val documentText = document.text
        if (end > documentText.length) return

        if (documentText.substring(start, end) != oldString) return
        document.replaceString(start, end, newString)
    }

    override fun getFamilyName() = KotlinBundle.message("replace.0.with.1", oldString, newString)
}