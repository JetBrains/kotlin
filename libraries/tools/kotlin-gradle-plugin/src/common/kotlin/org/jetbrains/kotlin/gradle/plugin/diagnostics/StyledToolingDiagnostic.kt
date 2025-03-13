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
    FATAL("⛔")
}

/**
 * Root sealed interface representing different types of diagnostic messages.
 *
 * Provides information about the diagnostic, including a name, a message describing the issue,
 * an optional solution, and optional documentation references.
 *
 * @property name The name of the diagnostic, providing a brief identifier for the issue.
 * @property message The descriptive message explaining the diagnostic or issue.
 * @property solution An optional proposed solution or recommended steps to resolve the issue.
 * @property documentation Optional documentation reference offering additional context or resources.
 */
internal sealed interface ToolingDiagnosticOutput {
    val name: String
    val message: String
    val solution: String?
    val documentation: String?
}

/**
 * Represents a diagnostic message in a styled form, intended for tools and plugins.
 */
internal interface StyledToolingDiagnostic : ToolingDiagnosticOutput

/**
 * Plain text representation of diagnostic messages.
 */
internal interface PlainTextToolingDiagnostic : ToolingDiagnosticOutput

/**
 * Abstract base class implementing the [ToolingDiagnosticOutput] interface. This class is responsible for
 * formatting and building diagnostic messages, solutions, documentation, and name representation
 * for a given [ToolingDiagnostic].
 *
 * @constructor Creates an instance of this abstract class.
 * @param diagnostic The [ToolingDiagnostic] instance containing details about the diagnostic issue.
 * @param showEmoji A flag indicating whether to display emoji icons for severity levels.
 * @param severity The severity level, overriding the diagnostic's original severity.
 */
private abstract class AbstractToolingDiagnostic(
    val diagnostic: ToolingDiagnostic,
    val showEmoji: Boolean,
    val severity: ToolingDiagnostic.Severity
) : ToolingDiagnosticOutput {
    override val name: String by lazy { buildName() }
    override val message: String by lazy { buildMessage() }
    override val solution: String? by lazy { buildSolution() }
    override val documentation: String? by lazy { buildDocumentation() }

    open fun buildName(): String = buildString {
        if (showEmoji) {
            val iconSeverity = when (severity) {
                WARNING -> DiagnosticIcon.WARNING
                ERROR -> DiagnosticIcon.ERROR
                FATAL -> DiagnosticIcon.FATAL
            }
            append(iconSeverity.icon)
            append(" ")
        }
        append(diagnostic.identifier.displayName)
    }.trimEnd()

    open fun buildMessage(): String = diagnostic.message.trimEnd()

    open fun buildSolution(): String? {
        val solutions = diagnostic.solutions
        return when (solutions.size) {
            0 -> null
            1 -> "Solution: ${solutions.single()}"
            else -> buildString {
                appendLine("Solutions:")
                solutions.forEach { solution ->
                    appendLine(" • $solution")
                }
            }.trimEnd()
        }
    }

    open fun buildDocumentation(): String? =
        diagnostic.documentation?.additionalUrlContext?.trimEnd()
}

/**
 * This class provides a styled implementation of the `StyledToolingDiagnostic` interface.
 *
 * It wraps a `ToolingDiagnostic` to present its fields in a styled format
 * through methods and properties like `name`, `message`, `solution`, and `documentation`.
 *
 * @constructor Creates an instance of `DefaultStyledToolingDiagnostic` using a `ToolingDiagnostic`.
 * @param diagnostic The `ToolingDiagnostic` instance containing raw diagnostic data.
 * @param showEmoji Indicates whether emoji-based severity icons should be included in the diagnostic output.
 * @param severity The severity level to apply to the styled output, overriding the diagnostic's original severity.
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
private class DefaultStyledToolingDiagnostic(
    diagnostic: ToolingDiagnostic,
    showEmoji: Boolean,
    severity: ToolingDiagnostic.Severity = diagnostic.severity
) : AbstractToolingDiagnostic(diagnostic, showEmoji, severity), StyledToolingDiagnostic {

    override fun buildName() = super.buildName()
        .bold()
        .applyColor(severity)

    override fun buildMessage() = buildString {
        if (!diagnostic.message.contains("```")) {
            appendLine(super.buildMessage())
        } else {
            processCodeBlocks(diagnostic.message.lines())
        }
    }.trimEnd()

    override fun buildSolution(): String? {
        val solutions = diagnostic.solutions
        if (solutions.isEmpty()) return null

        return buildString {
            val prefix = if (solutions.size == 1) "Solution" else "Solutions"
            appendLine("$prefix:".bold().green())

            when (solutions.size) {
                1 -> append(solutions.single().italic().green())
                else -> solutions.forEach { solution ->
                    appendLine(" • ${solution.italic()}".green())
                }
            }
        }.trimEnd()
    }

    override fun buildDocumentation(): String? = diagnostic.documentation?.let { doc ->
        val highlightedUrl = doc.url.blue()
        val parts = doc.additionalUrlContext.split(doc.url)
        return when (parts.size) {
            1 -> doc.additionalUrlContext.replace(doc.url, highlightedUrl).lightBlue()
            2 -> "${parts[0].lightBlue()}$highlightedUrl${parts[1].lightBlue()}"
            else -> doc.additionalUrlContext.lightBlue()
        }.trimEnd()
    }

    private fun String.applyColor(severity: ToolingDiagnostic.Severity) = when (severity) {
        WARNING -> yellow()
        ERROR, FATAL -> red()
    }

    private fun StringBuilder.processCodeBlocks(lines: List<String>) {
        var inCodeBlock = false
        for (line in lines) {
            when {
                line.trim() == "```" -> inCodeBlock = !inCodeBlock
                inCodeBlock -> appendLine(line.orange())
                else -> appendLine(line)
            }
        }
    }
}

/**
 * A concrete implementation of the `PlainTextToolingDiagnostic` interface, representing a tooling diagnostic message
 * formatted as plain text.
 *
 * This class extends `AbstractToolingDiagnostic`, inheriting its ability to construct details such as the name,
 * message, solutions, and documentation of a diagnostic. Additionally, it incorporates plain-text-specific formatting
 * as provided by the `PlainTextToolingDiagnostic` interface.
 *
 * @constructor Creates an instance of `DefaultPlainToolingDiagnostic` with a given diagnostic message and display preferences.
 * @param diagnostic The tooling diagnostic to be represented.
 * @param showEmoji Indicates whether emoji-based severity icons should be included in the diagnostic output.
 */
private class DefaultPlainToolingDiagnostic(
    diagnostic: ToolingDiagnostic,
    showEmoji: Boolean,
    severity: ToolingDiagnostic.Severity = diagnostic.severity
) : AbstractToolingDiagnostic(diagnostic, showEmoji, severity), PlainTextToolingDiagnostic

/**
 * Converts a `ToolingDiagnostic` into a styled representation using the `StyledToolingDiagnostic` interface.
 *
 * @param showEmoji A boolean flag indicating whether emoji-based severity icons should be included in the styled diagnostic output.
 * @return A `StyledToolingDiagnostic` instance containing the styled representation of the `ToolingDiagnostic`.
 * @return A styled diagnostic representation as a [StyledToolingDiagnostic].
 */
internal fun ToolingDiagnostic.styled(showEmoji: Boolean = true, severity: ToolingDiagnostic.Severity? = null): StyledToolingDiagnostic =
    DefaultStyledToolingDiagnostic(this, showEmoji, severity ?: this.severity)

/**
 * Converts the current instance of `ToolingDiagnostic` into a plain text diagnostic representation.
 *
 * @param showEmoji Determines whether emoji-based severity icons should be included in the plain text output.
 * @return A `PlainTextToolingDiagnostic` instance representing the diagnostic in plain text format.
 * @return A plain text representation of the [ToolingDiagnostic].
 */
internal fun ToolingDiagnostic.plain(showEmoji: Boolean = false, severity: ToolingDiagnostic.Severity? = null): PlainTextToolingDiagnostic =
    DefaultPlainToolingDiagnostic(this, showEmoji, severity ?: this.severity)
