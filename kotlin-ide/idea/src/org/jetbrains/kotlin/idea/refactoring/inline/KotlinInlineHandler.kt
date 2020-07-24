/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.inline

import com.intellij.lang.refactoring.InlineHandler
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.usageView.UsageInfo
import com.intellij.util.containers.MultiMap

class KotlinInlineHandler : InlineHandler {
    override fun prepareInlineElement(
        element: PsiElement,
        editor: Editor?,
        invokedOnReference: Boolean,
    ): InlineHandler.Settings = InlineHandler.Settings.CANNOT_INLINE_SETTINGS

    override fun removeDefinition(element: PsiElement, settings: InlineHandler.Settings) {}

    override fun createInliner(
        element: PsiElement,
        settings: InlineHandler.Settings,
    ): InlineHandler.Inliner = object : InlineHandler.Inliner {
        override fun getConflicts(reference: PsiReference, referenced: PsiElement) = MultiMap<PsiElement, String>(1).apply {
            putValue(reference.element, "Cannot inline reference from Kotlin")
        }

        override fun inlineUsage(usage: UsageInfo, referenced: PsiElement) {}
    }
}