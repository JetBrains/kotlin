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

import androidx.compose.compiler.plugins.kotlin.facade.AnalysisResult
import androidx.compose.compiler.plugins.kotlin.facade.SourceFile
import org.jetbrains.kotlin.checkers.DiagnosedRange
import org.jetbrains.kotlin.checkers.utils.CheckerTestUtil
import org.junit.Assert

abstract class AbstractComposeDiagnosticsTest : AbstractCompilerTest() {
    private class DiagnosticTestException(message: String) : Exception(message)

    protected fun check(expectedText: String, ignoreParseErrors: Boolean = false) {
        val diagnosedRanges: MutableList<DiagnosedRange> = ArrayList()
        val clearText = CheckerTestUtil.parseDiagnosedRanges(expectedText, diagnosedRanges)

        val errors = analyze(
            listOf(SourceFile("test.kt", clearText, ignoreParseErrors))
        ).diagnostics.toMutableList()

        // Ensure all the expected messages are there
        val message = StringBuilder()
        val found = mutableSetOf<AnalysisResult.Diagnostic>()
        for (range in diagnosedRanges) {
            for (diagnostic in range.getDiagnostics()) {
                val reportedDiagnostics = errors.filter { it.factoryName == diagnostic.name }
                if (reportedDiagnostics.isNotEmpty()) {
                    val reportedDiagnostic =
                        reportedDiagnostics.find {
                            it.textRanges.find {
                                it.startOffset == range.start && it.endOffset == range.end
                            } != null
                        }
                    if (reportedDiagnostic == null) {
                        val firstRange = reportedDiagnostics.first().textRanges.first()
                        message.append(
                            "  Error ${diagnostic.name} reported at ${
                                firstRange.startOffset
                            }-${firstRange.endOffset} but expected at ${range.start}-${range.end}\n"
                        )
                        message.append(
                            sourceInfo(clearText, firstRange.startOffset, firstRange.endOffset)
                        )
                    } else {
                        errors.remove(reportedDiagnostic)
                        found.add(reportedDiagnostic)
                    }
                } else {
                    message.append(
                        "  Diagnostic ${diagnostic.name} not reported, expected at ${
                            range.start
                        }\n"
                    )
                    message.append(
                        sourceInfo(clearText, range.start, range.end)
                    )
                }
            }
        }

        // Ensure only the expected errors are reported
        for (diagnostic in errors) {
            if (diagnostic !in found) {
                val range = diagnostic.textRanges.first()
                message.append(
                    "  Unexpected diagnostic ${diagnostic.factoryName} reported at ${
                        range.startOffset
                    }\n"
                )
                message.append(
                    sourceInfo(clearText, range.startOffset, range.endOffset)
                )
            }
        }

        // Throw an error if anything was found that was not expected
        if (message.isNotEmpty()) throw DiagnosticTestException("Mismatched errors:\n$message")
    }

    protected fun checkFail(expectedText: String) {
        Assert.assertThrows(DiagnosticTestException::class.java) {
            check(expectedText)
        }
    }

    private fun String.lineStart(offset: Int): Int {
        return this.lastIndexOf('\n', offset) + 1
    }

    private fun String.lineEnd(offset: Int): Int {
        val result = this.indexOf('\n', offset)
        return if (result < 0) this.length else result
    }

    // Return the source line that contains the given range with the range underlined with '~'s
    private fun sourceInfo(clearText: String, start: Int, end: Int): String {
        val lineStart = clearText.lineStart(start)
        val lineEnd = clearText.lineEnd(start)
        val displayEnd = if (end > lineEnd) lineEnd else end
        return "  " + clearText.substring(lineStart, lineEnd) + "\n" +
            " ".repeat(2 + start - lineStart) + "~".repeat(displayEnd - start) + "\n"
    }
}
