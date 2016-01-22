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

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.psi.KtFile

class AfterConversionPass(val project: Project, val postProcessor: PostProcessor) {
    fun run(kotlinFile: KtFile, range: TextRange?) {
        val rangeMarker = if (range != null) {
            val document = kotlinFile.viewProvider.document!!
            val marker = document.createRangeMarker(range.startOffset, range.endOffset)
            marker.isGreedyToLeft = true
            marker.isGreedyToRight = true
            marker
        }
        else {
            null
        }

        postProcessor.doAdditionalProcessing(kotlinFile, rangeMarker)
    }
}
