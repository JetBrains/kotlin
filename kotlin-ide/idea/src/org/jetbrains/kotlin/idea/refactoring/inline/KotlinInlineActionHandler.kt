/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.inline

import com.intellij.lang.Language
import com.intellij.lang.refactoring.InlineActionHandler
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.asJava.unwrapped
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
        inlineKotlinElement(project, editor, kotlinElement)
    }

    protected fun unwrapKotlinElement(element: PsiElement): KtElement? = element.unwrapped as? KtElement

    abstract fun canInlineKotlinElement(element: KtElement): Boolean
    abstract fun inlineKotlinElement(project: Project, editor: Editor?, element: KtElement)
}