/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.formatter

import com.intellij.openapi.progress.ProgressIndicatorProvider
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.formatter.trailingComma.TrailingCommaContext
import org.jetbrains.kotlin.idea.formatter.trailingComma.TrailingCommaState
import org.jetbrains.kotlin.idea.formatter.trailingComma.canAddTrailingComma
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFunctionLiteral
import org.jetbrains.kotlin.psi.KtTreeVisitorVoid

abstract class TrailingCommaVisitor : KtTreeVisitorVoid() {
    override fun visitKtElement(element: KtElement) {
        super.visitKtElement(element)
        // because KtFunctionLiteral contains KtParameterList
        if (element !is KtFunctionLiteral && element.canAddTrailingComma()) {
            runProcessIfApplicable(element)
        }
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
