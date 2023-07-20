/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.testbase

import org.gradle.testkit.runner.BuildResult
import org.jetbrains.kotlin.gradle.BaseGradleIT
import org.jetbrains.kotlin.gradle.internals.ENSURE_NO_KOTLIN_GRADLE_PLUGIN_ERRORS_TASK_NAME
import org.jetbrains.kotlin.gradle.internals.VERBOSE_DIAGNOSTIC_SEPARATOR
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.ToolingDiagnosticFactory
import kotlin.test.assertNull
import kotlin.test.assertTrue

fun BaseGradleIT.CompiledProject.assertHasDiagnostic(diagnosticFactory: ToolingDiagnosticFactory, withSubstring: String? = null) {
    output.assertHasDiagnostic(diagnosticFactory, withSubstring)
}

fun BaseGradleIT.CompiledProject.assertNoDiagnostic(diagnosticFactory: ToolingDiagnosticFactory, withSubstring: String? = null) {
    output.assertNoDiagnostic(diagnosticFactory, withSubstring)
}

fun BuildResult.assertHasDiagnostic(diagnosticFactory: ToolingDiagnosticFactory, withSubstring: String? = null) {
    output.assertHasDiagnostic(diagnosticFactory, withSubstring)
}

fun BuildResult.assertNoDiagnostic(diagnosticFactory: ToolingDiagnosticFactory, withSubstring: String? = null) {
    output.assertNoDiagnostic(diagnosticFactory, withSubstring)
}

fun String.assertHasDiagnostic(diagnosticFactory: ToolingDiagnosticFactory, withSubstring: String? = null) {
    val diagnosticsMessages = extractVerboselyRenderedDiagnostics(diagnosticFactory, this)
    assertTrue(diagnosticsMessages.isNotEmpty(), "Diagnostic with id=${diagnosticFactory.id} not found. Full text output:\n\n" + this)
    if (withSubstring != null) {
        assertTrue(
            diagnosticsMessages.any { withSubstring in it },
            "Diagnostic ${diagnosticFactory.id} doesn't have expected substring $withSubstring. " +
                    "Actual diagnostic messages with that ID:\n" +
                    diagnosticsMessages.joinToString(separator = "\n") +
                    "\nFull text output:\n\n" +
                    this
        )
    }
}

fun String.assertNoDiagnostic(diagnosticFactory: ToolingDiagnosticFactory, withSubstring: String? = null) {
    val diagnosticMessages = extractVerboselyRenderedDiagnostics(diagnosticFactory, this)
    if (withSubstring != null) {
        val matchedWithSubstring = diagnosticMessages.find { withSubstring in it }
        assertNull(
            matchedWithSubstring,
            "Diagnostic with id=${diagnosticFactory.id} and substring '${withSubstring}' was expected to be absent, but was reported. " +
                    "Actual diagnostic message: \n" +
                    matchedWithSubstring +
                    "\nFull text output:\n\n" +
                    this
        )
    } else {
        assertTrue(
            diagnosticMessages.isEmpty(),
            "Expected no diagnostics with id=${diagnosticFactory.id}, but some were reported:\n" +
                    diagnosticMessages.joinToString(separator = "\n") +
                    "\nFull text output:\n\n" +
                    this
        )
    }
}

/**
 * NB: Needs verbose mode of diagnostics, see [org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.internalVerboseDiagnostics]
 * Because this mode is enabled by the 'kotlin.internal'-property, actual output will always contain
 * [org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics.InternalKotlinGradlePluginPropertiesUsed].
 * For the sake of clarity, this diagnostic is filtered by default.
 */
fun BuildResult.extractProjectsAndTheirVerboseDiagnostics(): String = buildString {
    var diagnosticStarted = false
    val currentDiagnostic = mutableListOf<String>()

    fun startDiagnostic(line: String, lineIndex: Int) {
        require(!diagnosticStarted) {
            printBuildOutput()
            "Unexpected start of diagnostic $line on line ${lineIndex + 1}. The end of the previous diagnostic wasn't found yet"
        }

        currentDiagnostic += line
        diagnosticStarted = true
    }

    fun continueDiagnostic(line: String) {
        currentDiagnostic += line
    }

    fun endDiagnostic(line: String, lineIndex: Int) {
        require(diagnosticStarted) {
            printBuildOutput()
            "Unexpected end of diagnostic $line on line ${lineIndex + 1}"
        }

        currentDiagnostic += line

        // Suppress InternalKotlinGradlePluginProperties, but only if the single property it complains about is
        // 'kotlin.internal.verboseDiagnostics'
        val offendingProperties = currentDiagnostic.asSequence().filter { it.startsWith("kotlin.internal.") }
        if (KotlinToolingDiagnostics.InternalKotlinGradlePluginPropertiesUsed.id !in currentDiagnostic.first() ||
            offendingProperties.singleOrNull() != "kotlin.internal.verboseDiagnostics"
        ) {
            appendLine(currentDiagnostic.joinToString(separator = "\n", postfix = "\n"))
        }

        currentDiagnostic.clear()
        diagnosticStarted = false
    }


    for ((index, line) in output.lines().withIndex()) {
        when {
            line.trim() == VERBOSE_DIAGNOSTIC_SEPARATOR -> endDiagnostic(line, index)

            DIAGNOSTIC_START_REGEX.containsMatchIn(line) -> startDiagnostic(line, index)

            diagnosticStarted -> continueDiagnostic(line)

            line.startsWith(CONFIGURE_PROJECT_PREFIX)
                    || (line.contains(ENSURE_NO_KOTLIN_GRADLE_PLUGIN_ERRORS_TASK_NAME) && line.startsWith(TASK_EXECUTION_PREFIX)) -> {
                appendLine() // additional empty line between projects
                appendLine(line)
            }
        }
    }
}.trim()

/*
Expected format
      w: [DIAGNOSTIC_ID | WARNING] first line of diagnostic's text
or (fatals don't have 'w:' or 'e:' prefix):
      [DIAGNOSTIC_ID | FATAL] Fatal diagnostic
 */
private val DIAGNOSTIC_START_REGEX = """\s*([we]:)?\s*\[\w+ \| \w+].*""".toRegex()

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
private fun extractVerboselyRenderedDiagnostics(diagnostic: ToolingDiagnosticFactory, fromText: String): List<String> {
    var parsedPrefix = 0

    return generateSequence {
        extractNextVerboselyRenderedDiagnosticAndIndex(diagnostic, fromText, startIndex = parsedPrefix)
            ?.also { (_, newPrefix) -> parsedPrefix = newPrefix }
            ?.first
    }.toList()
}

// Returns diagnostic substring + index of the first symbol after the diagnostic message
private fun extractNextVerboselyRenderedDiagnosticAndIndex(
    diagnostic: ToolingDiagnosticFactory,
    fromText: String,
    startIndex: Int,
): Pair<String, Int>? {
    val diagnosticStartIndex = fromText.indexOf("[${diagnostic.id}", startIndex)
    if (diagnosticStartIndex == -1) return null

    val diagnosticHeaderEnd = fromText.indexOf("]", startIndex = diagnosticStartIndex)
    val diagnosticMessageStart = diagnosticHeaderEnd + 1

    val diagnosticSeparatorStartIndex = fromText.indexOf(VERBOSE_DIAGNOSTIC_SEPARATOR, startIndex = diagnosticStartIndex)
    // NB: substring's endIndex is exclusive, which gives us exactly the message
    val diagnosticMessage = fromText.substring(diagnosticMessageStart, diagnosticSeparatorStartIndex)
        .trim { it.isWhitespace() || it == '\n' }
    val indexOfFirstSymbolAfterSeparator = diagnosticSeparatorStartIndex + VERBOSE_DIAGNOSTIC_SEPARATOR.length
    return diagnosticMessage to indexOfFirstSymbolAfterSeparator
}

private const val CONFIGURE_PROJECT_PREFIX = "> Configure project"
private const val TASK_EXECUTION_PREFIX = "> Task"
