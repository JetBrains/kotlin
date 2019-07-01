/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.postProcessing

import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.RangeMarker
import com.intellij.psi.PsiElement
import kotlinx.coroutines.withContext
import org.jetbrains.kotlin.idea.core.util.EDT
import org.jetbrains.kotlin.idea.core.util.range
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.nj2k.NewJ2kConverterContext
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.elementsInRange


interface GeneralPostProcessing {
    suspend fun runProcessing(file: KtFile, rangeMarker: RangeMarker?, converterContext: NewJ2kConverterContext)
}

abstract class SimplePostProcessing() : GeneralPostProcessing {
    final override suspend fun runProcessing(file: KtFile, rangeMarker: RangeMarker?, converterContext: NewJ2kConverterContext) {
        withContext(EDT) {
            CommandProcessor.getInstance().runUndoTransparentAction {
                runWriteAction {
                    applySimpleProcessing(file, rangeMarker, converterContext)
                }
            }
        }
    }

    abstract fun applySimpleProcessing(file: KtFile, rangeMarker: RangeMarker?, converterContext: NewJ2kConverterContext)
}

abstract class ElementsBasedPostProcessing() : SimplePostProcessing() {
    final override fun applySimpleProcessing(file: KtFile, rangeMarker: RangeMarker?, converterContext: NewJ2kConverterContext) {
        val elements =
            rangeMarker?.let { marker ->
                val range = marker.range ?: return@let emptyList()
                file.elementsInRange(range)
            } ?: listOf(file)
        runProcessing(elements, converterContext)
    }

    abstract fun runProcessing(elements: List<PsiElement>, converterContext: NewJ2kConverterContext)
}

interface ProcessingGroup : GeneralPostProcessing

data class NamedPostProcessingGroup(
    val description: String,
    val processings: List<GeneralPostProcessing>
)

fun postProcessing(
    action: (file: KtFile, rangeMarker: RangeMarker?, converterContext: NewJ2kConverterContext) -> Unit
): GeneralPostProcessing =
    object : SimplePostProcessing() {
        override fun applySimpleProcessing(file: KtFile, rangeMarker: RangeMarker?, converterContext: NewJ2kConverterContext) {
            action(file, rangeMarker, converterContext)
        }
    }

