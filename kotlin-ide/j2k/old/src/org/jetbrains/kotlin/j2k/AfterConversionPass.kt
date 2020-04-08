/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.j2k

import com.intellij.openapi.editor.RangeMarker
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.psi.KtFile

class AfterConversionPass(val project: Project, val postProcessor: PostProcessor) {
    @JvmOverloads
    fun run(
        kotlinFile: KtFile,
        converterContext: ConverterContext?,
        range: TextRange?,
        onPhaseChanged: ((Int, String) -> Unit)? = null
    ) {
        postProcessor.doAdditionalProcessing(
            when {
                range != null -> JKPieceOfCodePostProcessingTarget(kotlinFile, range.toRangeMarker(kotlinFile))
                else -> JKMultipleFilesPostProcessingTarget(listOf(kotlinFile))
            },
            converterContext,
            onPhaseChanged
        )
    }
}

fun TextRange.toRangeMarker(file: KtFile): RangeMarker =
    runReadAction { file.viewProvider.document!!.createRangeMarker(startOffset, endOffset) }.apply {
        isGreedyToLeft = true
        isGreedyToRight = true
    }