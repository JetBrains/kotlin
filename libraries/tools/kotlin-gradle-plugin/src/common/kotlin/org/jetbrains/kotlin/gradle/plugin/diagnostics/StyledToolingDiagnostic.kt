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
enum class DiagnosticIcon(val icon: String) {
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
interface StyledToolingDiagnostic {
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
private class StyledToolingDiagnosticImp(private val diagnostic: ToolingDiagnostic) : StyledToolingDiagnostic {
    override val name: String get() = buildName()
    override val message: String get() = buildMessage()
    override val solution: String? get() = buildSolution()
    override val documentation: String? get() = buildDocumentation()

    private fun buildName(): String {
        val icon = when (diagnostic.severity) {
            WARNING -> DiagnosticIcon.WARNING
            else -> DiagnosticIcon.ERROR
        }
        return buildString {
            append(icon.icon)
            append(" ")
            append(diagnostic.identifier.displayName.bold().let {
                when (diagnostic.severity) {
                    WARNING -> it.yellow()
                    ERROR, FATAL -> it.red()
                }
            })
        }
    }

    private fun buildMessage(): String {
        // Optional: Early return for messages without code blocks
        if (!diagnostic.message.contains("```")) {
            return diagnostic.message.bold()
        }

        var inCodeBlock = false
        val lines = diagnostic.message.lines()
        return buildString {
            for (line in lines) {
                when {
                    line.trim() == "```" -> {
                        inCodeBlock = !inCodeBlock
                        continue
                    }
                    inCodeBlock -> appendLine(line.orange())
                    else -> appendLine(line.bold())
                }
            }
        }.trimEnd()
    }

    private fun buildSolution(): String? {
        val solutions = diagnostic.solutions
        if (solutions.isEmpty()) return null

        return buildString {
            val prefix = if (solutions.size == 1) "Solution" else "Solutions"
            appendLine("$prefix:".bold().green())

            if (solutions.size == 1) {
                append(solutions.single().italic().green())
            } else {
                solutions.forEach { solution ->
                    appendLine(" • ${solution.italic()}".green())
                }
            }
        }.trimEnd()
    }

    private fun buildDocumentation(): String? =
        diagnostic.documentation?.let {
            val highLightedUrl = it.url.blue()
            it.additionalUrlContext.replace(it.url, highLightedUrl).lightBlue()
        }
}

internal fun ToolingDiagnostic.styled(): StyledToolingDiagnostic = StyledToolingDiagnosticImp(this)
