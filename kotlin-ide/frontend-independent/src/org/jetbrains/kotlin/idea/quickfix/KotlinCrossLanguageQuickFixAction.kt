/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.FileModificationService
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

abstract class KotlinCrossLanguageQuickFixAction<out T : PsiElement>(element: T) : QuickFixActionBase<T>(element) {
    override val isCrossLanguageFix: Boolean
        get() = true

    override final fun invoke(project: Project, editor: Editor?, file: PsiFile) {
        val element = element
        if (element != null && FileModificationService.getInstance().prepareFileForWrite(element.containingFile)) {
            invokeImpl(project, editor, file)
        }
    }

    protected abstract fun invokeImpl(project: Project, editor: Editor?, file: PsiFile)
}
