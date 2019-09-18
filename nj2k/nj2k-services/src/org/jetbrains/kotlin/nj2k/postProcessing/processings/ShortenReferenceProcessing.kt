/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.postProcessing.processings

import com.intellij.openapi.editor.RangeMarker
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.nj2k.ImportStorage
import org.jetbrains.kotlin.nj2k.NewJ2kConverterContext
import org.jetbrains.kotlin.nj2k.postProcessing.GeneralPostProcessing
import org.jetbrains.kotlin.nj2k.postProcessing.runUndoTransparentActionInEdt
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtQualifiedExpression

class ShortenReferenceProcessing : GeneralPostProcessing {
    private val filter = filter@{ element: PsiElement ->
        when (element) {
            is KtQualifiedExpression -> when {
                ImportStorage.isImportNeededForCall(element) -> ShortenReferences.FilterResult.PROCESS
                else -> ShortenReferences.FilterResult.SKIP
            }
            else -> ShortenReferences.FilterResult.PROCESS
        }
    }

    override fun runProcessing(file: KtFile, rangeMarker: RangeMarker?, converterContext: NewJ2kConverterContext) {
        runUndoTransparentActionInEdt(inWriteAction = false) {
            if (rangeMarker != null) {
                if (rangeMarker.isValid) {
                    ShortenReferences.DEFAULT.process(file, rangeMarker.startOffset, rangeMarker.endOffset, filter)
                }
            } else {
                ShortenReferences.DEFAULT.process(file, filter)
            }
        }
    }
}
