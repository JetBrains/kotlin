/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.postProcessing.processings

import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.RangeMarker
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.idea.util.ActionRunningMode
import org.jetbrains.kotlin.nj2k.JKImportStorage
import org.jetbrains.kotlin.nj2k.NewJ2kConverterContext
import org.jetbrains.kotlin.nj2k.postProcessing.FileBasedPostProcessing
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtQualifiedExpression

class ShortenReferenceProcessing : FileBasedPostProcessing() {
    private val filter = filter@{ element: PsiElement ->
        when (element) {
            is KtQualifiedExpression -> when {
                JKImportStorage.isImportNeededForCall(element) -> ShortenReferences.FilterResult.PROCESS
                else -> ShortenReferences.FilterResult.SKIP
            }
            else -> ShortenReferences.FilterResult.PROCESS
        }
    }

    override fun runProcessing(file: KtFile, allFiles: List<KtFile>, rangeMarker: RangeMarker?, converterContext: NewJ2kConverterContext) {
        CommandProcessor.getInstance().runUndoTransparentAction {
            if (rangeMarker != null) {
                if (rangeMarker.isValid) {
                    ShortenReferences.DEFAULT.process(
                        file,
                        rangeMarker.startOffset,
                        rangeMarker.endOffset,
                        filter,
                        actionRunningMode = ActionRunningMode.RUN_IN_EDT
                    )
                }
            } else {
                ShortenReferences.DEFAULT.process(file, filter, actionRunningMode = ActionRunningMode.RUN_IN_EDT)
            }
        }
    }
}
