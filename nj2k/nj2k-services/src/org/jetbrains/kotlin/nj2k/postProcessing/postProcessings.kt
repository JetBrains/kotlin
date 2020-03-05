/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k.postProcessing

import com.intellij.openapi.editor.RangeMarker
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.j2k.JKMultipleFilesPostProcessingTarget
import org.jetbrains.kotlin.j2k.JKPieceOfCodePostProcessingTarget
import org.jetbrains.kotlin.j2k.JKPostProcessingTarget
import org.jetbrains.kotlin.j2k.elements
import org.jetbrains.kotlin.nj2k.NewJ2kConverterContext
import org.jetbrains.kotlin.psi.KtFile


interface GeneralPostProcessing {
    val options: PostProcessingOptions
        get() = PostProcessingOptions.DEFAULT

    fun runProcessing(target: JKPostProcessingTarget, converterContext: NewJ2kConverterContext)
}

data class PostProcessingOptions(
    val disablePostprocessingFormatting: Boolean = true
) {
    companion object {
        val DEFAULT = PostProcessingOptions()
    }
}


abstract class FileBasedPostProcessing : GeneralPostProcessing {
    final override fun runProcessing(target: JKPostProcessingTarget, converterContext: NewJ2kConverterContext) = when (target) {
        is JKPieceOfCodePostProcessingTarget ->
            runProcessing(target.file, listOf(target.file), target.rangeMarker, converterContext)
        is JKMultipleFilesPostProcessingTarget ->
            target.files.forEach { file ->
                runProcessing(file, target.files, rangeMarker = null, converterContext = converterContext)
            }
    }

    abstract fun runProcessing(file: KtFile, allFiles: List<KtFile>, rangeMarker: RangeMarker?, converterContext: NewJ2kConverterContext)
}

abstract class ElementsBasedPostProcessing : GeneralPostProcessing {
    final override fun runProcessing(target: JKPostProcessingTarget, converterContext: NewJ2kConverterContext) {
        runProcessing(target.elements(), converterContext)
    }

    abstract fun runProcessing(elements: List<PsiElement>, converterContext: NewJ2kConverterContext)
}

data class NamedPostProcessingGroup(
    val description: String,
    val processings: List<GeneralPostProcessing>
)
