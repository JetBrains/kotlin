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
private class StyledToolingDiagnosticImp(private val diagnostic: ToolingDiagnostic, val colored: Boolean) : StyledToolingDiagnostic {
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
            append(diagnostic.identifier.displayName.bold(colored).let {
                when (diagnostic.severity) {
                    WARNING -> it.yellow(colored)
                    ERROR, FATAL -> it.red(colored)
                }
            })
        }
    }

    private fun buildMessage(): String {
        return buildString {
            appendLine(diagnostic.identifier.displayName.bold(colored))
            if (!diagnostic.message.contains("```")) {
                appendLine(diagnostic.message.bold(colored))
            } else {
                var inCodeBlock = false
                val lines = diagnostic.message.lines()

                appendLine(diagnostic.identifier.displayName.bold(colored))
                for (line in lines) {
                    when {
                        line.trim() == "```" -> {
                            inCodeBlock = !inCodeBlock
                            continue
                        }
                        inCodeBlock -> appendLine(line.orange(colored))
                        else -> appendLine(line)
                    }
                }
            }
        }.trimEnd()
    }

    private fun buildSolution(): String? {
        val solutions = diagnostic.solutions
        if (solutions.isEmpty()) return null

        return buildString {
            val prefix = if (solutions.size == 1) "Solution" else "Solutions"
            appendLine("$prefix:".bold(colored).green(colored))

            if (solutions.size == 1) {
                append(solutions.single().italic(colored).green(colored))
            } else {
                solutions.forEach { solution ->
                    appendLine(" • ${solution.italic(colored)}".green(colored))
                }
            }
        }.trimEnd()
    }

    private fun buildDocumentation(): String? =
        diagnostic.documentation?.let {
            if (!colored) {
                return it.additionalUrlContext
            }

            val highLightedUrl = it.url.blue()
            val splitted = it.additionalUrlContext.split(it.url)
            when (splitted.size) {
                1 -> it.additionalUrlContext.replace(it.url, highLightedUrl).lightBlue()
                2 -> buildString {
                    append(splitted[0].lightBlue())
                    append(highLightedUrl)
                    append(splitted[1].lightBlue())
                }
                else -> it.additionalUrlContext.lightBlue()
            }
        }
}

internal fun ToolingDiagnostic.styled(colored: Boolean): StyledToolingDiagnostic = StyledToolingDiagnosticImp(this, colored)
