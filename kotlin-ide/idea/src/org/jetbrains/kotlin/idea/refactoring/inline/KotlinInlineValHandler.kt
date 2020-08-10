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

package org.jetbrains.kotlin.idea.refactoring.inline

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.refactoring.HelpID
import com.intellij.refactoring.RefactoringBundle
import com.intellij.refactoring.util.CommonRefactoringUtil
import org.jetbrains.annotations.Nls
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.findUsages.ReferencesSearchScopeHelper
import org.jetbrains.kotlin.idea.refactoring.inline.KotlinInlinePropertyProcessor.Companion.extractInitialization
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtProperty

class KotlinInlineValHandler(private val withPrompt: Boolean) : KotlinInlineActionHandler() {
    constructor() : this(withPrompt = true)

    override fun canInlineKotlinElement(element: KtElement): Boolean = element is KtProperty && element.name != null

    @Nls
    private fun getKind(declaration: KtProperty): String = if (declaration.isLocal)
        KotlinBundle.message("text.variable")
    else
        KotlinBundle.message("text.property")

    override fun inlineKotlinElement(project: Project, editor: Editor?, element: KtElement) {
        val declaration = element as KtProperty
        val name = declaration.name!!

        val file = declaration.containingKtFile
        if (file.isCompiled) {
            return showErrorHint(
                project,
                editor,
                KotlinBundle.message("error.hint.text.cannot.inline.0.from.a.decompiled.file", name)
            )
        }

        val getter = declaration.getter?.takeIf { it.hasBody() }
        val setter = declaration.setter?.takeIf { it.hasBody() }
        if ((getter != null || setter != null) && declaration.initializer != null) {
            return showErrorHint(
                project,
                editor,
                KotlinBundle.message("cannot.inline.property.with.accessor.s.and.backing.field")
            )
        }

        if (ReferencesSearchScopeHelper.search(declaration).findFirst() == null) {
            return showErrorHint(project, editor, KotlinBundle.message("0.1.is.never.used", getKind(declaration).capitalize(), name))
        }

        var assignmentToDelete: KtBinaryExpression? = null
        if (getter == null && setter == null) {
            val initializer = extractInitialization(declaration).getInitializerOrShowErrorHint(project, editor) ?: return
            assignmentToDelete = initializer.assignment
        }

        performRefactoring(declaration, assignmentToDelete, editor)
    }

    private fun performRefactoring(
        declaration: KtProperty,
        assignmentToDelete: KtBinaryExpression?,
        editor: Editor?
    ) {
        val reference = editor?.findSimpleNameReference()
        val dialog = KotlinInlineValDialog(declaration, reference, assignmentToDelete, withPreview = withPrompt, editor)
        if (withPrompt && !ApplicationManager.getApplication().isUnitTestMode && dialog.shouldBeShown()) {
            dialog.show()
        } else {
            dialog.doAction()
        }
    }

    companion object {
        fun showErrorHint(project: Project, editor: Editor?, @Nls message: String) {
            CommonRefactoringUtil.showErrorHint(
                project,
                editor,
                message,
                RefactoringBundle.message("inline.variable.title"),
                HelpID.INLINE_VARIABLE
            )
        }
    }
}
