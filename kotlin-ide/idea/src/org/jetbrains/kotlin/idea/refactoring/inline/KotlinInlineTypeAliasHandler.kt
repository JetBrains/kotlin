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
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtTypeAlias

class KotlinInlineTypeAliasHandler : KotlinInlineActionHandler() {
    override val helpId: String? get() = HelpID.INLINE_VARIABLE

    override val refactoringName: String get() = KotlinBundle.message("title.inline.type.alias")

    override fun canInlineKotlinElement(element: KtElement): Boolean = element is KtTypeAlias

    override fun inlineKotlinElement(project: Project, editor: Editor?, element: KtElement) {
        val typeAlias = element as? KtTypeAlias ?: return
        if (!checkSources(project, editor, element)) return

        typeAlias.name ?: return
        typeAlias.getTypeReference() ?: return

        val dialog = KotlinInlineTypeAliasDialog(
            element,
            editor?.findSimpleNameReference(),
            editor = editor,
        )

        if (!ApplicationManager.getApplication().isUnitTestMode) {
            dialog.show()
        } else {
            dialog.performOKAction()
        }
    }
}
