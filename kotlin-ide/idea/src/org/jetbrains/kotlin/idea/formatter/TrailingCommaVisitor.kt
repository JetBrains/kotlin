/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.formatter

import com.intellij.openapi.progress.ProgressIndicatorProvider
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.formatter.trailingComma.addTrailingCommaIsAllowedForThis
import org.jetbrains.kotlin.psi.*

abstract class TrailingCommaVisitor : KtTreeVisitorVoid() {
    override fun visitParameterList(list: KtParameterList) {
        super.visitParameterList(list)
        runProcessIfAllowed(list)
    }

    override fun visitValueArgumentList(list: KtValueArgumentList) {
        super.visitValueArgumentList(list)
        runProcessIfAllowed(list)
    }

    override fun visitArrayAccessExpression(expression: KtArrayAccessExpression) {
        super.visitArrayAccessExpression(expression)
        runProcessIfAllowed(expression.indicesNode)
    }

    override fun visitTypeParameterList(list: KtTypeParameterList) {
        super.visitTypeParameterList(list)
        runProcessIfAllowed(list)
    }

    override fun visitTypeArgumentList(typeArgumentList: KtTypeArgumentList) {
        super.visitTypeArgumentList(typeArgumentList)
        runProcessIfAllowed(typeArgumentList)
    }

    override fun visitCollectionLiteralExpression(expression: KtCollectionLiteralExpression) {
        super.visitCollectionLiteralExpression(expression)
        runProcessIfAllowed(expression)
    }

    override fun visitWhenEntry(jetWhenEntry: KtWhenEntry) {
        super.visitWhenEntry(jetWhenEntry)
        runProcessIfAllowed(jetWhenEntry)
    }

    override fun visitDestructuringDeclaration(destructuringDeclaration: KtDestructuringDeclaration) {
        super.visitDestructuringDeclaration(destructuringDeclaration)
        runProcessIfAllowed(destructuringDeclaration)
    }

    override fun visitElement(element: PsiElement) {
        ProgressIndicatorProvider.checkCanceled()

        if (recursively) super.visitElement(element)
    }

    private fun runProcessIfAllowed(element: KtElement) {
        if (element.addTrailingCommaIsAllowedForThis()) {
            process(element)
        }
    }

    protected abstract fun process(commaOwner: KtElement)

    protected open val recursively: Boolean = true
}
