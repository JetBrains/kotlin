package org.jetbrains.kotlin.idea.refactoring.inline

import com.intellij.lang.refactoring.InlineHandler
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiType
import com.intellij.usageView.UsageInfo
import com.intellij.util.containers.MultiMap
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.util.isUnitLiteral
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import org.jetbrains.uast.*

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
        return MultiMap<PsiElement, String>(1).apply {
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
    }

    open fun performInline(usage: UsageInfo, referenced: PsiElement) = Unit
}

fun isEmptyMethodWithReturnWithNull(element: PsiElement): Boolean {
    val uMethod = element.toUElementOfType<UMethod>() ?: return false
    if (uMethod.uastParameters.isNotEmpty()) return false
    val uBlockExpression = uMethod.uastBody as? UBlockExpression ?: return false
    val uReturnExpression = uBlockExpression.expressions.singleOrNull() as? UReturnExpression ?: return false
    return uReturnExpression.returnExpression?.isNullLiteral() == true
}

fun isMethodWithEmptyBody(element: PsiElement): Boolean {
    val uMethod = element.toUElementOfType<UMethod>() ?: return false
    if (uMethod.uastParameters.isNotEmpty() || uMethod.returnType != PsiType.VOID) return false
    val uBlockExpression = uMethod.uastBody as? UBlockExpression ?: return false
    val uReturnExpression = uBlockExpression.expressions.ifEmpty { return true }.singleOrNull() as? UReturnExpression ?: return false
    return isReturnWithoutExpression(uReturnExpression)
}

fun isReturnWithoutExpression(uReturnExpression: UReturnExpression): Boolean {
    val uExpression = uReturnExpression.returnExpression ?: return true

    /**
     * fun a() = Unit
     * fun a() {
     *     return Unit
     * }
     */
    return uExpression.isKotlinUnitLiteral
}

val UExpression.isKotlinUnitLiteral: Boolean get() = sourcePsi?.safeAs<KtExpression>()?.isUnitLiteral == true
