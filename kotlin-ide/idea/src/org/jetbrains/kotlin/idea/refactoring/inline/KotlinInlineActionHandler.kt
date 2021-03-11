/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.inline

import com.intellij.lang.Language
import com.intellij.lang.findUsages.DescriptiveNameUtil
import com.intellij.lang.refactoring.InlineActionHandler
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts
import com.intellij.psi.PsiElement
import com.intellij.refactoring.util.CommonRefactoringUtil
import org.jetbrains.kotlin.asJava.unwrapped
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtElement

abstract class KotlinInlineActionHandler : InlineActionHandler() {
    override fun isEnabledForLanguage(language: Language) = language == KotlinLanguage.INSTANCE
    final override fun canInlineElement(element: PsiElement): Boolean {
        val kotlinElement = unwrapKotlinElement(element) ?: return false
        return canInlineKotlinElement(kotlinElement)
    }

    final override fun inlineElement(project: Project, editor: Editor?, element: PsiElement) {
        val kotlinElement = unwrapKotlinElement(element) ?: error("Kotlin element not found")
        KotlinInlineRefactoringFUSCollector.log(elementFrom = kotlinElement, languageTo = KotlinLanguage.INSTANCE, isCrossLanguage = false)
        inlineKotlinElement(project, editor, kotlinElement)
    }

    override fun getActionName(element: PsiElement?): String = refactoringName

    abstract fun canInlineKotlinElement(element: KtElement): Boolean
    abstract fun inlineKotlinElement(project: Project, editor: Editor?, element: KtElement)
    abstract val refactoringName: @NlsContexts.DialogTitle String

    open val helpId: String? = null

    fun showErrorHint(project: Project, editor: Editor?, @NlsContexts.DialogMessage message: String) {
        CommonRefactoringUtil.showErrorHint(project, editor, message, refactoringName, helpId)
    }

    fun checkSources(project: Project, editor: Editor?, declaration: KtElement): Boolean = !declaration.containingKtFile.isCompiled.also {
        if (it) {
            val declarationName = DescriptiveNameUtil.getDescriptiveName(declaration)
            showErrorHint(
                project,
                editor,
                KotlinBundle.message("error.hint.text.cannot.inline.0.from.a.decompiled.file", declarationName)
            )
        }
    }
}

private fun unwrapKotlinElement(element: PsiElement): KtElement? {
    val ktElement = element.unwrapped as? KtElement
    return ktElement?.navigationElement as? KtElement ?: ktElement
}
