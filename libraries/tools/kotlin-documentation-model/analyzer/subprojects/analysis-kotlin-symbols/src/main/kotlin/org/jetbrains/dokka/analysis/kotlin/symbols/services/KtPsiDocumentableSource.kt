/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.symbols.services

import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import org.jetbrains.dokka.model.DocumentableSource
import org.jetbrains.kotlin.lexer.KtTokens


internal class KtPsiDocumentableSource(val psi: PsiElement?) : DocumentableSource {
    override val path = psi?.containingFile?.virtualFile?.path ?: ""

    override fun computeLineNumber(): Int? {
        return psi?.let {
                val range = it.node?.findChildByType(KtTokens.IDENTIFIER)?.textRange ?: it.textRange
                val doc = PsiDocumentManager.getInstance(it.project).getDocument(it.containingFile)
                // IJ uses 0-based line-numbers; external source browsers use 1-based
                doc?.getLineNumber(range.startOffset)?.plus(1)
            }
    }
}
