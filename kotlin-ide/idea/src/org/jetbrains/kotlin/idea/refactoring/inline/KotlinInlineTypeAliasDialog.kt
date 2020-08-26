/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.inline

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.help.HelpManager
import com.intellij.psi.PsiReference
import com.intellij.refactoring.HelpID
import org.jetbrains.kotlin.idea.refactoring.KotlinRefactoringSettings
import org.jetbrains.kotlin.psi.KtTypeAlias

class KotlinInlineTypeAliasDialog(
    typeAlias: KtTypeAlias,
    reference: PsiReference?,
    editor: Editor?,
) : AbstractKotlinInlineDialog<KtTypeAlias>(typeAlias, reference, editor) {
    init {
        init()
    }

    override fun doHelpAction() = HelpManager.getInstance().invokeHelp(HelpID.INLINE_VARIABLE)

    override fun isInlineThis() = KotlinRefactoringSettings.instance.INLINE_TYPE_ALIAS_THIS

    public override fun doAction() {
        invokeRefactoring(
            KotlinInlineTypeAliasProcessor(
                declaration = declaration,
                reference = reference,
                inlineThisOnly = isInlineThisOnly,
                deleteAfter = !isInlineThisOnly && !isKeepTheDeclaration,
                editor = editor,
                project = project,
            )
        )

        val settings = KotlinRefactoringSettings.instance
        if (myRbInlineThisOnly.isEnabled && myRbInlineAll.isEnabled) {
            settings.INLINE_TYPE_ALIAS_THIS = isInlineThisOnly
        }
    }
}
