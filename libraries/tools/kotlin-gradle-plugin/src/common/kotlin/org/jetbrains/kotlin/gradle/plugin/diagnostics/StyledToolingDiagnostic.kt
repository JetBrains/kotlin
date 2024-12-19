/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics

import org.jetbrains.kotlin.gradle.plugin.diagnostics.TerminalColorSupport.TerminalStyle.blue
import org.jetbrains.kotlin.gradle.plugin.diagnostics.TerminalColorSupport.TerminalStyle.bold
import org.jetbrains.kotlin.gradle.plugin.diagnostics.TerminalColorSupport.TerminalStyle.green
import org.jetbrains.kotlin.gradle.plugin.diagnostics.TerminalColorSupport.TerminalStyle.italic
import org.jetbrains.kotlin.gradle.plugin.diagnostics.TerminalColorSupport.TerminalStyle.lightBlue
import org.jetbrains.kotlin.gradle.plugin.diagnostics.TerminalColorSupport.TerminalStyle.orange
import org.jetbrains.kotlin.gradle.plugin.diagnostics.TerminalColorSupport.TerminalStyle.red
import org.jetbrains.kotlin.gradle.plugin.diagnostics.TerminalColorSupport.TerminalStyle.yellow
import org.jetbrains.kotlin.gradle.plugin.diagnostics.ToolingDiagnostic.Severity.*

/**
 * Represents diagnostic icons used to indicate the severity level of diagnostics.
 *
 * @property icon The visual representation of the diagnostic icon, such as a warning or error symbol.
 */
internal enum class DiagnosticIcon(val icon: String) {
    WARNING("⚠️"),
    ERROR("❌"),
}

/**
 * Represents a diagnostic message in a styled form, intended for tools and plugins.
 *
 * Provides information about the diagnostic, including a name, a message describing the issue,
 * an optional solution, and optional documentation references.
 *
 * @property name The name of the diagnostic, providing a brief identifier for the issue.
 * @property message The descriptive message explaining the diagnostic or issue.
 * @property solution An optional proposed solution or recommended steps to resolve the issue.
 * @property documentation Optional documentation reference offering additional context or resources.
 */
internal interface StyledToolingDiagnostic {
    val name: String
    val message: String
    val solution: String?
    val documentation: String?
}

/**
 * This class provides a styled implementation of the `StyledToolingDiagnostic` interface.
 *
 * It wraps a `ToolingDiagnostic` to present its fields in a styled format
 * through methods and properties like `name`, `message`, `solution`, and `documentation`.
 *
 * @constructor Creates an instance of `StyledToolingDiagnosticImp` using a `ToolingDiagnostic`.
 * @param diagnostic The `ToolingDiagnostic` instance containing raw diagnostic data.
 *
 * The following details are styled:
 * - The `name` is constructed with a severity-based icon and a colored identifier name.
 * - The `message` is formatted with bold text.
 * - The `solution` is presented in a formatted list or as a single-line message, with green styling.
 * - The `documentation` is styled in blue, if available.
 *
 * Severity-based styling:
 * - `WARNING`: Yellow text styling for the identifier name.
 * - `ERROR` or `FATAL`: Red text styling for the identifier name.
 *
 * Solution presentation:
 * - If one solution is present, it is labeled "Solution" and italicized.
 * - If multiple solutions exist, each is listed with a bullet point, italicized, and styled in green.
 */
private class StyledToolingDiagnosticImp(
    private val diagnostic: ToolingDiagnostic,
    private val colored: Boolean,
    private val showEmoji: Boolean
) : StyledToolingDiagnostic {
    override val name: String by lazy { buildName() }
    override val message: String by lazy { buildMessage() }
    override val solution: String? by lazy { buildSolution() }
    override val documentation: String? by lazy { buildDocumentation() }

    private fun buildName(): String = buildString {
        if (showEmoji) {
            val icon = when (diagnostic.severity) {
                WARNING -> DiagnosticIcon.WARNING
                else -> DiagnosticIcon.ERROR
            }

            append(icon.icon)
            append(" ")
        }
        append(
            diagnostic.identifier.displayName
                .bold(colored)
                .applyColor(diagnostic.severity)
        )
    }

    private fun buildMessage(): String = buildString {
        if (!diagnostic.message.contains("```")) {
            appendLine(diagnostic.message.bold(colored))
        } else {
            processCodeBlocks(diagnostic.message.lines())
        }
    }.trimEnd()

    private fun buildSolution(): String? {
        val solutions = diagnostic.solutions
        if (solutions.isEmpty()) return null

        return buildString {
            val prefix = if (solutions.size == 1) "Solution" else "Solutions"
            appendLine("$prefix:".bold(colored).green(colored))

            when (solutions.size) {
                1 -> append(solutions.single().italic(colored).green(colored))
                else -> solutions.forEach { solution ->
                    appendLine(" • ${solution.italic(colored)}".green(colored))
                }
            }
        }.trimEnd()
    }

    private fun buildDocumentation(): String? = diagnostic.documentation?.let { doc ->
        if (!colored) return doc.additionalUrlContext

        val highlightedUrl = doc.url.blue()
        val parts = doc.additionalUrlContext.split(doc.url)
        return when (parts.size) {
            1 -> doc.additionalUrlContext.replace(doc.url, highlightedUrl).lightBlue()
            2 -> "${parts[0].lightBlue()}$highlightedUrl${parts[1].lightBlue()}"
            else -> doc.additionalUrlContext.lightBlue()
        }
    }

    private fun String.applyColor(severity: ToolingDiagnostic.Severity) = when (severity) {
        WARNING -> yellow(colored)
        ERROR, FATAL -> red(colored)
    }

    private fun StringBuilder.processCodeBlocks(lines: List<String>) {
        var inCodeBlock = false
        for (line in lines) {
            when {
                line.trim() == "```" -> inCodeBlock = !inCodeBlock
                inCodeBlock -> appendLine(line.orange(colored))
                else -> appendLine(line)
            }
        }
    }
}

internal fun ToolingDiagnostic.styled(colored: Boolean, showEmoji: Boolean): StyledToolingDiagnostic =
    StyledToolingDiagnosticImp(this, colored, showEmoji)
