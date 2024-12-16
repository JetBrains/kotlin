/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics

import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi
import java.net.URI

@InternalKotlinGradlePluginApi // used in integration tests
abstract class ToolingDiagnosticFactory(
    private val predefinedSeverity: ToolingDiagnostic.Severity,
    private val predefinedGroup: DiagnosticGroup,
) {
    open val id: String = this::class.simpleName!!

    /**
     * Builds a diagnostic object with the specified severity, diagnostic group, throwable,
     * and a builder function for constructing additional diagnostic details.
     *
     * @param severity The severity level of the diagnostic (e.g., WARNING, ERROR, or FATAL).
     *        Defaults to `predefinedSeverity` if not specified.
     * @param group The diagnostic group to which the diagnostic belongs. Defaults to `predefinedGroup` if not specified.
     * @param throwable An optional `Throwable` associated with the diagnostic, providing further context or details.
     * @param builder A lambda function for building additional details of the diagnostic message, starting from the title step.
     */
    protected fun build(
        severity: ToolingDiagnostic.Severity? = null,
        group: DiagnosticGroup? = null,
        throwable: Throwable? = null,
        builder: ToolingDiagnostics.TitleStep.() -> ToolingDiagnostics.OptionalStep,
    ) = ToolingDiagnostics.diagnostic(
        id = id,
        group = group ?: predefinedGroup,
        severity = severity ?: predefinedSeverity,
        throwable = throwable,
        builder = builder
    )

    /**
     * Builds a diagnostic object with the provided details and optional metadata.
     *
     * @param title The title of the diagnostic message, providing a concise summary of the issue.
     * @param description A detailed description of the diagnostic issue, explaining its nature and context.
     * @param solutions A list of potential solutions or actions to address the diagnostic issue.
     * @param documentationUrl An optional URI pointing to related documentation or resources for additional context.
     * @param documentationHint A lambda function that generates a hint string with the provided documentation URL.
     *                          Defaults to "See $it for more details."
     * @param severity The severity level of the diagnostic (e.g., WARNING, ERROR, or FATAL). Defaults to `predefinedSeverity` if not specified.
     * @param group The diagnostic group to which the diagnostic belongs. Defaults to `predefinedGroup` if not specified.
     * @param throwable An optional throwable providing further context or information about the diagnostic issue.
     */
    protected fun buildDiagnostic(
        title: String,
        description: String,
        solutions: List<String>,
        documentationUrl: URI? = null,
        documentationHint: (String) -> String = { "See $it for more details." },
        severity: ToolingDiagnostic.Severity? = null,
        group: DiagnosticGroup? = null,
        throwable: Throwable? = null,
    ) = build(severity, group, throwable) {
        title(title)
            .description(description)
            .solutions { solutions }
            .apply {
                documentationUrl?.let { documentationLink(it, documentationHint) }
            }
    }

    /**
     * Builds a diagnostic object with the provided title, description, a single solution, and optional metadata.
     *
     * @param title The title of the diagnostic message, providing a concise summary of the issue.
     * @param description A detailed description of the diagnostic issue, explaining its nature and context.
     * @param solution A single potential solution or action to address the diagnostic issue.
     * @param documentationUrl An optional URI pointing to related documentation or resources for additional context.
     * @param documentationHint A lambda function that generates a hint string with the provided documentation URL.
     *                          Defaults to "See $it for more details."
     * @param severity The severity level of the diagnostic (e.g., WARNING, ERROR, or FATAL).
     * @param group The diagnostic group to which the diagnostic belongs.
     * @param throwable An optional throwable providing further context or information about the diagnostic issue.
     */
    protected fun buildDiagnostic(
        title: String,
        description: String,
        solution: String,  // Single solution overload
        documentationUrl: URI? = null,
        documentationHint: (String) -> String = { "See $it for more details." },
        severity: ToolingDiagnostic.Severity? = null,
        group: DiagnosticGroup? = null,
        throwable: Throwable? = null,
    ) = buildDiagnostic(
        title,
        description,
        listOf(solution),
        documentationUrl,
        documentationHint,
        severity,
        group,
        throwable
    )
}

/**
 * Interface for building tooling diagnostics in the Kotlin Gradle Plugin.
 *
 * This builder provides methods to set various attributes of a tooling diagnostic,
 * including its name, message, solutions, and documentation.
 */
@InternalKotlinGradlePluginApi
object ToolingDiagnostics {
    @InternalKotlinGradlePluginApi
    interface TitleStep {
        fun title(value: String): DescriptionStep
        fun title(value: () -> String): DescriptionStep
    }

    @InternalKotlinGradlePluginApi
    interface DescriptionStep {
        fun description(value: String): SolutionStep
        fun description(value: () -> String): SolutionStep
    }

    @InternalKotlinGradlePluginApi
    interface SolutionStep {
        fun solution(value: String): OptionalStep
        fun solutions(vararg values: String): OptionalStep
        fun solution(value: () -> String): OptionalStep
        fun solutions(values: () -> List<String>): OptionalStep
    }

    @InternalKotlinGradlePluginApi
    interface OptionalStep {
        fun documentationLink(
            url: URI,
            textWithUrl: (String) -> String = { "See $url for more details." },
        ): OptionalStep
    }

    private data class BuilderState(
        val id: String,
        val group: DiagnosticGroup,
        val severity: ToolingDiagnostic.Severity,
        val throwable: Throwable?,
        val title: String? = null,
        val description: String? = null,
        val solutions: List<String> = emptyList(),
        val documentation: ToolingDiagnostic.Documentation? = null,
    ) {
        init {
            require(solutions.all { it.isNotBlank() }) { "Solutions cannot be blank" }
        }
    }

    private class BuilderImpl(
        id: String,
        group: DiagnosticGroup,
        severity: ToolingDiagnostic.Severity,
        throwable: Throwable? = null,
    ) : TitleStep, DescriptionStep, SolutionStep, OptionalStep {
        private var state = BuilderState(
            id = id,
            group = group,
            severity = severity,
            throwable = throwable
        )

        override fun title(value: () -> String) = title(value())
        override fun title(value: String) = apply {
            require(value.isNotBlank()) { "Title cannot be blank" }
            state = state.copy(title = value)
        }

        override fun description(value: () -> String) = description(value())
        override fun description(value: String) = apply {
            state = state.copy(description = value)
        }

        override fun solution(value: () -> String) = solution(value())
        override fun solution(value: String) = apply {
            checkSolutionIsSingleLine(value)
            state = state.copy(solutions = state.solutions + value)
        }

        override fun solutions(values: () -> List<String>) = solutions(*values().toTypedArray())
        override fun solutions(vararg values: String) = apply {
            values.forEach { checkSolutionIsSingleLine(it) }
            state = state.copy(solutions = state.solutions + values)
        }

        override fun documentationLink(
            url: URI,
            textWithUrl: (String) -> String,
        ) = apply {
            state = state.copy(
                documentation = ToolingDiagnostic.Documentation(url.toString(), textWithUrl(url.toString()))
            )
        }

        fun build() = ToolingDiagnostic(
            identifier = ToolingDiagnostic.ID(
                state.id,
                checkNotNull(state.title) { "Title is required for diagnostic with ID ${state.id}" },
                state.group
            ),
            message = checkNotNull(state.description) { "Description is required for diagnostic with ID ${state.id}" },
            severity = checkNotNull(state.severity) { "Severity is required for diagnostic with ID ${state.id}" },
            solutions = state.solutions,
            documentation = state.documentation,
            throwable = state.throwable
        )

        override fun toString(): String = buildString {
            appendLine("DiagnosticBuilder(")
            appendLine(build().toString())
            appendLine(")")
        }

        private fun checkSolutionIsSingleLine(text: String) {
            require(text.lines().size == 1) { "Solution should not be multi-line: $text" }
        }
    }

    internal fun diagnostic(
        id: String,
        group: DiagnosticGroup,
        severity: ToolingDiagnostic.Severity,
        throwable: Throwable? = null,
        builder: TitleStep.() -> OptionalStep,
    ) = BuilderImpl(id, group, severity, throwable).apply { builder() }.build()
}