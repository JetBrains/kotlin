/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.inline

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.refactoring.HelpID
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.refactoring.inline.KotlinInlinePropertyProcessor.Companion.extractInitialization
import org.jetbrains.kotlin.idea.refactoring.isAbstract
import org.jetbrains.kotlin.idea.util.isExpectDeclaration
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtProperty

class KotlinInlinePropertyHandler(private val withPrompt: Boolean = true) : KotlinInlineActionHandler() {
    override val helpId: String? get() = HelpID.INLINE_VARIABLE

    override val refactoringName: String get() = KotlinBundle.message("title.inline.property")

    override fun canInlineKotlinElement(element: KtElement): Boolean = element is KtProperty && element.name != null

    override fun inlineKotlinElement(project: Project, editor: Editor?, element: KtElement) {
        val declaration = element as KtProperty
        if (!checkSources(project, editor, element)) return

        if (!element.hasBody()) {
            val message = when {
                element.isAbstract() -> KotlinBundle.message("refactoring.cannot.be.applied.to.abstract.declaration", refactoringName)
                element.isExpectDeclaration() -> KotlinBundle.message(
                    "refactoring.cannot.be.applied.to.expect.declaration",
                    refactoringName
                )
                else -> null
            }

            if (message != null) {
                showErrorHint(project, editor, message)
                return
            }
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
        editor: Editor?,
    ) {
        val reference = editor?.findSimpleNameReference()
        val dialog = KotlinInlinePropertyDialog(
            property = declaration,
            reference = reference,
            assignmentToDelete = assignmentToDelete,
            withPreview = withPrompt,
            editor = editor
        )

        if (withPrompt && !ApplicationManager.getApplication().isUnitTestMode && dialog.shouldBeShown()) {
            dialog.show()
        } else {
            dialog.performOKAction()
        }
    }
}
