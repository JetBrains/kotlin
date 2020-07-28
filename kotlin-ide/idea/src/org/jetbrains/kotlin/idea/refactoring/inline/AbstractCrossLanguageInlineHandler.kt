package org.jetbrains.kotlin.idea.refactoring.inline

import com.intellij.lang.refactoring.InlineHandler
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.usageView.UsageInfo
import com.intellij.util.containers.MultiMap
import org.jetbrains.kotlin.idea.KotlinBundle

abstract class AbstractCrossLanguageInlineHandler : InlineHandler {
    override fun prepareInlineElement(
        element: PsiElement,
        editor: Editor?,
        invokedOnReference: Boolean,
    ): InlineHandler.Settings? = null

    override fun removeDefinition(element: PsiElement, settings: InlineHandler.Settings) = Unit

    override fun createInliner(
        element: PsiElement,
        settings: InlineHandler.Settings,
    ): InlineHandler.Inliner = object : InlineHandler.Inliner {
        override fun getConflicts(reference: PsiReference, referenced: PsiElement) = MultiMap<PsiElement, String>(1).apply {
            val psiElement = reference.element
            putValue(
                psiElement,
                KotlinBundle.message(
                    "text.cannot.inline.reference.from.0.to.1",
                    referenced.language.displayName,
                    psiElement.language.displayName,
                ),
            )
        }

        override fun inlineUsage(usage: UsageInfo, referenced: PsiElement) = Unit
    }
}