package com.jetbrains.kotlin.structuralsearch.sanity

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.kdoc.psi.impl.KDocSection
import org.jetbrains.kotlin.psi.*
import kotlin.random.Random

object SanityTestElementPicker {

    private val random = Random(System.currentTimeMillis())
    private const val KEEP_RATIO = 0.7

    /** Tells which Kotlin [PsiElement]-s can be used directly or not (i.e. some child) as a SSR pattern. */
    private val PsiElement.isSearchable: Boolean
        get() = when (this) {
            is PsiWhiteSpace, is KtPackageDirective, is KtImportList -> false
            is KtParameterList, is KtValueArgumentList, is KtSuperTypeList, is KtTypeArgumentList, is KtTypeParameterList,
            is KtBlockExpression, is KtClassBody -> this.children.any()
            is KtModifierList, is KtDeclarationModifierList -> false
            is LeafPsiElement, is KtOperationReferenceExpression -> false
            is KtLiteralStringTemplateEntry, is KtEscapeStringTemplateEntry -> false
            is KDocSection -> false
            else -> true
        }

    private fun shouldStopAt(element: PsiElement) = when (element) {
        is KtAnnotationEntry -> true
        else -> false
    }

    private fun mustContinueAfter(element: PsiElement) = when (element) {
        is KtParameterList, is KtValueArgumentList, is KtSuperTypeList, is KtTypeArgumentList, is KtTypeParameterList -> true
        is KtClassBody, is KtBlockExpression, is KtBlockCodeFragment -> true
        is KtPrimaryConstructor -> true
        is KtParameter -> true
        is KtSimpleNameStringTemplateEntry, is KtBlockStringTemplateEntry, is KtSuperTypeCallEntry -> true
        is KtClassOrObject -> element.body?.children?.size ?: 0 > 4
        else -> false
    }

    /** Returns a random [PsiElement] whose text can be used as a pattern against [tree]. */
    fun pickFrom(tree: Array<PsiElement>): PsiElement? {
        if (tree.isEmpty()) return null
        var element = tree.filter { it.isSearchable }.random()

        var canContinue: Boolean
        var mustContinue: Boolean
        do {
            val searchableChildren = element.children.filter { it.isSearchable }
            if (searchableChildren.isEmpty()) break

            element = searchableChildren.random()

            canContinue = element.children.any { it.isSearchable } && !shouldStopAt(element)
            mustContinue = random.nextFloat() > KEEP_RATIO || mustContinueAfter(element)
        } while (canContinue && mustContinue)

        return element
    }

}