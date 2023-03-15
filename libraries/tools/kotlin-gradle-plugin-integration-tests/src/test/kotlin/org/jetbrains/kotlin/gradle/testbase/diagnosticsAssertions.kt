/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

import org.gradle.testkit.runner.BuildResult
import org.jetbrains.kotlin.gradle.BaseGradleIT
import org.jetbrains.kotlin.gradle.internals.VERBOSE_DIAGNOSTIC_SEPARATOR
import org.jetbrains.kotlin.gradle.plugin.diagnostics.ToolingDiagnosticFactory
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

fun BaseGradleIT.CompiledProject.assertHasDiagnostic(diagnosticFactory: ToolingDiagnosticFactory, withSubstring: String? = null) {
    output.assertHasDiagnostic(diagnosticFactory, withSubstring)
}

fun BaseGradleIT.CompiledProject.assertNoDiagnostic(diagnosticFactory: ToolingDiagnosticFactory) {
    output.assertNoDiagnostic(diagnosticFactory)
}

fun String.assertHasDiagnostic(diagnosticFactory: ToolingDiagnosticFactory, withSubstring: String? = null) {
    val diagnosticMessage = extractVerboselyRenderedDiagnostic(diagnosticFactory, this)
    assertNotNull(diagnosticMessage) { "Diagnostic with id=${diagnosticFactory.id} not found" }
    if (withSubstring != null) {
        assertTrue(
            withSubstring in diagnosticMessage,
            "Diagnostic ${diagnosticFactory.id} doesn't have expected substring $withSubstring. " +
                    "Actual diagnostic message:\n" +
                    diagnosticMessage
        )
    }
}

fun String.assertNoDiagnostic(diagnosticFactory: ToolingDiagnosticFactory) {
    val diagnosticMessage = extractVerboselyRenderedDiagnostic(diagnosticFactory, this)
    assertNull(
        diagnosticMessage,
        "Diagnostic with id=${diagnosticFactory.id} was expected to be absent, but was reported. " +
                "Actual diagnostic message: \n" +
                diagnosticMessage
    )
}

/**
 * NB: Needs verbose mode of diagnostics, see [org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.internalVerboseDiagnostics]
 */
fun BuildResult.extractProjectsAndTheirVerboseDiagnostics(): String = buildString {
    var diagnosticStarted = false
    for ((index, line) in output.lines().withIndex()) {
        when {
            line.trim() == VERBOSE_DIAGNOSTIC_SEPARATOR -> {
                if (diagnosticStarted) {
                    appendLine(line)
                    appendLine()
                    diagnosticStarted = false
                } else {
                    printBuildOutput()
                    error("Unexpected end of diagnostic $line on line ${index + 1}")
                }
            }

            DIAGNOSTIC_START_REGEX.matches(line) -> {
                if (!diagnosticStarted) {
                    appendLine(line)
                    diagnosticStarted = true
                } else {
                    printBuildOutput()
                    error(
                        "Unexpected start of diagnostic $line on line ${index + 1}. The end of the previous diagnostic wasn't found yet"
                    )
                }
            }

            diagnosticStarted -> {
                appendLine(line)
            }

            line.startsWith(CONFIGURE_PROJECT_PREFIX) -> {
                appendLine() // additional empty line between projects
                appendLine(line)
            }
        }
    }
}.trim()

/*
Expected format
      w: [DIAGNOSTIC_ID | WARNING] first line of diagnostic's text
 */
private val DIAGNOSTIC_START_REGEX = """\s*[we]:\s*\[[^\[]*].*""".toRegex()


/*
 Expected format:

       diagnosticStartIndex     diagnosticHeaderEndIndex
        |                        |
        v                        v
     w: [MyDiagnosticId | WARNING] Some
     Multiline
         Text
     #diagnostic-end
     ^
     |
     diagnosticSeparatorStartIndex

 Expected result for this case:
     "Some
     Multiline
         Text"
  */
private fun extractVerboselyRenderedDiagnostic(diagnostic: ToolingDiagnosticFactory, fromText: String): String? {
    val diagnosticStartIndex = fromText.indexOf("[${diagnostic.id}")
    if (diagnosticStartIndex == -1) return null

    val diagnosticHeaderEnd = fromText.indexOf("]", startIndex = diagnosticStartIndex)
    val diagnosticMessageStart = diagnosticHeaderEnd + 1

    val diagnosticSeparatorStartIndex = fromText.indexOf(VERBOSE_DIAGNOSTIC_SEPARATOR, startIndex = diagnosticStartIndex)
    // NB: substring's endIndex is exclusive, which gives us exactly the message
    return fromText.substring(diagnosticMessageStart, diagnosticSeparatorStartIndex).trim { it.isWhitespace() || it == '\n' }
}

private const val CONFIGURE_PROJECT_PREFIX = "> Configure project"
