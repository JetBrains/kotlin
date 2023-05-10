/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.compiler.plugins.kotlin

import androidx.compose.compiler.plugins.kotlin.facade.SourceFile
import org.jetbrains.kotlin.checkers.DiagnosedRange
import org.jetbrains.kotlin.checkers.utils.CheckerTestUtil
import org.jetbrains.kotlin.utils.addToStdlib.flatGroupBy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows

abstract class AbstractComposeDiagnosticsTest : AbstractCompilerTest() {
    protected fun check(expectedText: String, ignoreParseErrors: Boolean = false) {
        val diagnosedRanges: MutableList<DiagnosedRange> = ArrayList()
        val clearText = CheckerTestUtil.parseDiagnosedRanges(expectedText, diagnosedRanges)

        val errors = analyze(
            listOf(SourceFile("test.kt", clearText, ignoreParseErrors))
        ).diagnostics

        val rangeToDiagnostics = errors.flatGroupBy { it.textRanges }.mapValues { entry ->
            entry.value.map { it.factoryName }.toSet()
        }
        val startOffsetToGroups = rangeToDiagnostics.entries.groupBy(
            keySelector = { it.key.startOffset },
            valueTransform = { it.value }
        )
        val endOffsetsToGroups = rangeToDiagnostics.entries.groupBy(
            keySelector = { it.key.endOffset },
            valueTransform = { it.value }
        )

        val annotatedText = buildString {
            for ((i, c) in clearText.withIndex()) {
                endOffsetsToGroups[i]?.let { groups ->
                    repeat(groups.size) { append("<!>") }
                }
                startOffsetToGroups[i]?.let { groups ->
                    for (diagnostics in groups) {
                        append("<!${diagnostics.joinToString(",")}!>")
                    }
                }
                append(c)
            }
        }

        assertEquals(expectedText, annotatedText)
    }

    protected fun checkFail(expectedText: String) {
        assertThrows(AssertionError::class.java) {
            check(expectedText)
        }
    }
}
