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

import org.jetbrains.kotlin.checkers.utils.CheckerTestUtil
import org.jetbrains.kotlin.checkers.DiagnosedRange
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.TopDownAnalyzerFacadeForJVM
import org.jetbrains.kotlin.cli.jvm.compiler.NoScopeRecordCliBindingTrace
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.diagnostics.Diagnostic
import java.io.File

abstract class AbstractComposeDiagnosticsTest : AbstractCompilerTest() {

    fun doTest(expectedText: String) {
        doTest(expectedText, myEnvironment!!)
    }

    fun doTest(expectedText: String, environment: KotlinCoreEnvironment) {
        val diagnosedRanges: MutableList<DiagnosedRange> = ArrayList()
        val clearText = CheckerTestUtil.parseDiagnosedRanges(expectedText, diagnosedRanges)
        val file =
            createFile("test.kt", clearText, environment.project)
        val files = listOf(file)

        // Use the JVM version of the analyzer to allow using classes in .jar files
        val moduleTrace = NoScopeRecordCliBindingTrace()
        val result = TopDownAnalyzerFacadeForJVM.analyzeFilesWithJavaIntegration(
            environment.project,
            files,
            moduleTrace,
            environment.configuration.copy().apply {
                this.put(JVMConfigurationKeys.JVM_TARGET, JvmTarget.JVM_1_8)
            },
            environment::createPackagePartProvider
        )

        // Collect the errors
        val errors = result.bindingContext.diagnostics.all().toMutableList()

        val message = StringBuilder()

        // Ensure all the expected messages are there
        val found = mutableSetOf<Diagnostic>()
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
                            sourceInfo(
                                clearText,
                                firstRange.startOffset, firstRange.endOffset,
                                "  "
                            )
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
                        sourceInfo(
                            clearText,
                            range.start,
                            range.end,
                            "  "
                        )
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
                    sourceInfo(
                        clearText,
                        range.startOffset,
                        range.endOffset,
                        "  "
                    )
                )
            }
        }

        // Throw an error if anything was found that was not expected
        if (message.length > 0) throw Exception("Mismatched errors:\n$message")
    }
}

fun assertExists(file: File): File {
    if (!file.exists()) {
        throw IllegalStateException("'$file' does not exist. Run test from gradle")
    }
    return file
}

fun String.lineStart(offset: Int): Int {
    return this.lastIndexOf('\n', offset) + 1
}

fun String.lineEnd(offset: Int): Int {
    val result = this.indexOf('\n', offset)
    return if (result < 0) this.length else result
}

// Return the source line that contains the given range with the range underlined with '~'s
fun sourceInfo(clearText: String, start: Int, end: Int, prefix: String = ""): String {
    val lineStart = clearText.lineStart(start)
    val lineEnd = clearText.lineEnd(start)
    val displayEnd = if (end > lineEnd) lineEnd else end
    return prefix + clearText.substring(lineStart, lineEnd) + "\n" +
        prefix + " ".repeat(start - lineStart) + "~".repeat(displayEnd - start) + "\n"
}