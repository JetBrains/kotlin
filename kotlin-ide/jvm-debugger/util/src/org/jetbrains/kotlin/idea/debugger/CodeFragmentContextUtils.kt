/*
 * Copyright 2000-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger

import com.intellij.psi.*
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.idea.debugger.KotlinEditorTextProvider.Companion.isAcceptedAsCodeFragmentContext
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.parents

fun getContextElement(elementAt: PsiElement?): PsiElement? {
    if (elementAt == null) return null

    if (elementAt is PsiCodeBlock) {
        return getContextElement(elementAt.context?.context)
    }

    if (elementAt is KtLightClass) {
        return getContextElement(elementAt.kotlinOrigin)
    }

    val containingFile = elementAt.containingFile
    if (containingFile is PsiJavaFile) {
        return elementAt
    }

    if (containingFile !is KtFile) {
        return null
    }

    val accurateElement = getAccurateContextElement(elementAt, containingFile)
    if (accurateElement != null) {
        return accurateElement
    }

    return containingFile
}

private fun getAccurateContextElement(elementAt: PsiElement, containingFile: KtFile): PsiElement? {
    // elementAt can be PsiWhiteSpace when codeFragment is created from line start offset (in case of first opening EE window)
    val elementAtSkippingWhitespaces = getElementSkippingWhitespaces(elementAt)

    if (elementAtSkippingWhitespaces is LeafPsiElement && elementAtSkippingWhitespaces.elementType == KtTokens.RBRACE) {
        val classBody = elementAtSkippingWhitespaces.parent as? KtClassBody
        val classOrObject = classBody?.parent as? KtClassOrObject
        var declarationParent = classOrObject?.parent
        if (declarationParent is KtObjectLiteralExpression) {
            declarationParent = declarationParent.parent
        }

        if (declarationParent != null) {
            return getAccurateContextElement(declarationParent, containingFile)
        }
    }

    val lineStartOffset = elementAtSkippingWhitespaces.textOffset

    val targetExpression = PsiTreeUtil.findElementOfClassAtOffset(containingFile, lineStartOffset, KtExpression::class.java, false)

    if (targetExpression != null) {
        if (isAcceptedAsCodeFragmentContext(targetExpression)) {
            return targetExpression
        }

        KotlinEditorTextProvider.findExpressionInner(elementAt, true)
            ?.takeIf { isAcceptedAsCodeFragmentContext(it) }
            ?.let { return it }

        targetExpression.parents
            .firstOrNull { isAcceptedAsCodeFragmentContext(it) }
            ?.let { return it }
    }

    return null
}

private fun getElementSkippingWhitespaces(elementAt: PsiElement): PsiElement {
    if (elementAt is PsiWhiteSpace || elementAt is PsiComment) {
        val newElement = PsiTreeUtil.skipSiblingsForward(elementAt, PsiWhiteSpace::class.java, PsiComment::class.java)
        if (newElement != null) {
            return newElement
        }
    }

    return elementAt
}