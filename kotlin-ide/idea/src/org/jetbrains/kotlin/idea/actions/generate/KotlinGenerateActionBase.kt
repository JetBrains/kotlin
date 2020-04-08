/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.actions.generate

import com.intellij.codeInsight.CodeInsightActionHandler
import com.intellij.codeInsight.actions.CodeInsightAction
import com.intellij.lang.ContextAwareActionHandler
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.idea.refactoring.canRefactor
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType

abstract class KotlinGenerateActionBase : CodeInsightAction(), CodeInsightActionHandler {
    override fun update(
        presentation: Presentation,
        project: Project,
        editor: Editor,
        file: PsiFile,
        dataContext: DataContext,
        actionPlace: String?
    ) {
        super.update(presentation, project, editor, file, dataContext, actionPlace)
        val actionHandler = handler
        if (actionHandler is ContextAwareActionHandler && presentation.isEnabled) {
            presentation.isEnabled = actionHandler.isAvailableForQuickList(editor, file, dataContext)
        }
    }

    override fun isValidForFile(project: Project, editor: Editor, file: PsiFile): Boolean {
        if (file !is KtFile || file.isCompiled) return false

        val targetClass = getTargetClass(editor, file) ?: return false
        return targetClass.canRefactor() && isValidForClass(targetClass)
    }

    protected open fun getTargetClass(editor: Editor, file: PsiFile): KtClassOrObject? {
        return file.findElementAt(editor.caretModel.offset)?.getNonStrictParentOfType<KtClassOrObject>()
    }

    protected abstract fun isValidForClass(targetClass: KtClassOrObject): Boolean

    override fun startInWriteAction() = false

    override fun getHandler() = this
}