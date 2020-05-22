/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.formatter

import com.intellij.openapi.progress.ProgressIndicatorProvider
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.formatter.trailingComma.TrailingCommaContext
import org.jetbrains.kotlin.idea.formatter.trailingComma.TrailingCommaState
import org.jetbrains.kotlin.psi.*

abstract class TrailingCommaVisitor : KtTreeVisitorVoid() {
    override fun visitParameterList(list: KtParameterList) {
        super.visitParameterList(list)
        runProcessIfApplicable(list)
    }

    override fun visitValueArgumentList(list: KtValueArgumentList) {
        super.visitValueArgumentList(list)
        runProcessIfApplicable(list)
    }

    override fun visitArrayAccessExpression(expression: KtArrayAccessExpression) {
        super.visitArrayAccessExpression(expression)
        runProcessIfApplicable(expression.indicesNode)
    }

    override fun visitTypeParameterList(list: KtTypeParameterList) {
        super.visitTypeParameterList(list)
        runProcessIfApplicable(list)
    }

    override fun visitTypeArgumentList(typeArgumentList: KtTypeArgumentList) {
        super.visitTypeArgumentList(typeArgumentList)
        runProcessIfApplicable(typeArgumentList)
    }

    override fun visitCollectionLiteralExpression(expression: KtCollectionLiteralExpression) {
        super.visitCollectionLiteralExpression(expression)
        runProcessIfApplicable(expression)
    }

    override fun visitWhenEntry(jetWhenEntry: KtWhenEntry) {
        super.visitWhenEntry(jetWhenEntry)
        runProcessIfApplicable(jetWhenEntry)
    }

    override fun visitDestructuringDeclaration(destructuringDeclaration: KtDestructuringDeclaration) {
        super.visitDestructuringDeclaration(destructuringDeclaration)
        runProcessIfApplicable(destructuringDeclaration)
    }

    override fun visitElement(element: PsiElement) {
        ProgressIndicatorProvider.checkCanceled()

        if (recursively) super.visitElement(element)
    }

    private fun runProcessIfApplicable(element: KtElement) {
        val context = TrailingCommaContext.create(element)
        if (context.state != TrailingCommaState.NOT_APPLICABLE) {
            process(context)
        }
    }

    /**
     * [trailingCommaContext] doesn't contain a state [TrailingCommaState.NOT_APPLICABLE]
     */
    protected abstract fun process(trailingCommaContext: TrailingCommaContext)

    protected open val recursively: Boolean = true
}