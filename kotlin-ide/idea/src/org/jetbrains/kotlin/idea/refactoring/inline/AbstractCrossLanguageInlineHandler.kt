package org.jetbrains.kotlin.idea.refactoring.inline

import com.intellij.lang.refactoring.InlineHandler
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.usageView.UsageInfo
import com.intellij.util.containers.MultiMap
import org.jetbrains.kotlin.idea.KotlinBundle

abstract class AbstractCrossLanguageInlineHandler : InlineHandler {
    final override fun prepareInlineElement(
        element: PsiElement,
        editor: Editor?,
        invokedOnReference: Boolean,
    ): InlineHandler.Settings? = null

    final override fun removeDefinition(element: PsiElement, settings: InlineHandler.Settings) = Unit

    final override fun createInliner(
        element: PsiElement,
        settings: InlineHandler.Settings,
    ): InlineHandler.Inliner = object : InlineHandler.Inliner {
        override fun getConflicts(
            reference: PsiReference,
            referenced: PsiElement
        ): MultiMap<PsiElement, String> = prepareReference(reference, referenced)

        override fun inlineUsage(usage: UsageInfo, referenced: PsiElement) = performInline(usage, referenced)
    }

    open fun prepareReference(reference: PsiReference, referenced: PsiElement): MultiMap<PsiElement, String> {
        val psiElement = reference.element
        return createMultiMapWithSingleConflict(
            psiElement,
            KotlinBundle.message(
                "text.cannot.inline.reference.from.0.to.1",
                referenced.language.displayName,
                psiElement.language.displayName,
            ),
        )
    }

    open fun performInline(usage: UsageInfo, referenced: PsiElement) = Unit

    companion object {
        fun createMultiMapWithSingleConflict(
            element: PsiElement,
            message: String
        ): MultiMap<PsiElement, String> = MultiMap<PsiElement, String>(1).apply {
            putValue(element, message)
        }
    }
}
