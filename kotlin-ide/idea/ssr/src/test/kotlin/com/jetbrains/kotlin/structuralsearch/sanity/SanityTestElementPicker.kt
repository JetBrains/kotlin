package com.jetbrains.kotlin.structuralsearch.sanity

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.psi.*
import kotlin.random.Random

object SanityTestElementPicker {

    private val random = Random(System.currentTimeMillis())
    private const val KEEP_RATIO = 0.2

    /** Tells which Kotlin [PsiElement]-s can be used directly or not (i.e. some child) as a SSR pattern. */
    private val PsiElement.isSearchable: Boolean
        get() = when (this) {
            is PsiWhiteSpace, is KtPackageDirective, is KtImportList -> false
            is KtModifierList, is KtDeclarationModifierList -> false
            is LeafPsiElement -> false
            else -> true
        }

    private fun shouldStopAt(element: PsiElement) = when (element) {
        is KtAnnotationEntry -> true
        else -> false
    }

    private fun mustContinueAfter(element: PsiElement) = when (element) {
        is KtParameterList, is KtValueArgumentList, is KtSuperTypeList, is KtTypeArgumentList, is KtTypeParameterList -> true
        is KtClassBody, is KtBlockExpression, is KtBlockStringTemplateEntry, is KtBlockCodeFragment -> true
        is KtPrimaryConstructor -> true
        is KtParameter -> true
        else -> false
    }

    /** Returns a random [PsiElement] whose text can be used as a pattern against [tree]. */
    fun pickFrom(tree: Array<PsiElement>): PsiElement {
        var element = tree.filter { it.isSearchable }.random()

        while (element.children.any() && !shouldStopAt(element) && (random.nextFloat() > KEEP_RATIO || mustContinueAfter(element))) {
            val searchableChildren = element.children.filter { it.isSearchable }
            if (searchableChildren.isEmpty()) break

            val newElement = searchableChildren.random()
            if (newElement.children.none() && (!element.isSearchable || mustContinueAfter(newElement))) break
            element = newElement
        }

        return element
    }

}