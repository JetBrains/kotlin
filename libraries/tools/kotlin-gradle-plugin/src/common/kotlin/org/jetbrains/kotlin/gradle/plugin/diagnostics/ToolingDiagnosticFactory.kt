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
) {
    open val id: String = this::class.simpleName!!

    /**
     * Builds a diagnostic message with required name, message, and solution.
     *
     * @param severity Optional severity level. If not provided, uses predefined severity.
     * @param throwable Optional throwable to attach to the diagnostic.
     * @param builder Builder block that must provide name, message, and at least one solution.
     * @return Built [ToolingDiagnostic]
     * @throws IllegalStateException if required fields are missing
     */
    protected fun build(
        severity: ToolingDiagnostic.Severity? = null,
        throwable: Throwable? = null,
        builder: ToolingDiagnostics.TitleStep.() -> ToolingDiagnostics.OptionalStep,
    ) = ToolingDiagnostics.diagnostic(
        id = id,
        severity = severity ?: predefinedSeverity,
        throwable = throwable,
        builder = builder
    )

    /**
     * Builds a diagnostic message with specified title, description, and possible solutions.
     *
     * @param title The title of the diagnostic message.
     * @param description A detailed description of the diagnostic issue.
     * @param solutions A vararg list of potential solutions or steps to address the issue.
     * @param documentationUrl An optional URL pointing to related documentation or context.
     * @param documentationHint A lambda that generates a hint string using the provided documentation URL.
     * @param severity The severity of the diagnostic message. Defaults to the predefined severity if not provided.
     * @param throwable An optional throwable associated with the diagnostic issue.
     */
    protected fun buildDiagnostic(
        title: String,
        description: String,
        solutions: List<String>,
        documentationUrl: URI? = null,
        documentationHint: (String) -> String = { "See $it for more details." },
        severity: ToolingDiagnostic.Severity? = null,
        throwable: Throwable? = null,
    ) = build(severity, throwable) {
        title(title)
            .description(description)
            .solutions { solutions }
            .apply {
                documentationUrl?.let { documentationLink(it, documentationHint) }
            }
    }

    /**
     * Builds a diagnostic message with a single solution overload.
     *
     * @param title The title of the diagnostic message.
     * @param description A detailed description of the diagnostic issue.
     * @param solution A single potential solution or step to address the issue.
     * @param documentationUrl An optional URL pointing to related documentation or context.
     * @param documentationHint A lambda that generates a hint string using the provided documentation URL.
     * @param severity The severity of the diagnostic message. Defaults to the predefined severity if not provided.
     * @param throwable An optional throwable associated with the diagnostic issue.
     */
    protected fun buildDiagnostic(
        title: String,
        description: String,
        solution: String,  // Single solution overload
        documentationUrl: URI? = null,
        documentationHint: (String) -> String = { "See $it for more details." },
        severity: ToolingDiagnostic.Severity? = null,
        throwable: Throwable? = null,
    ) = buildDiagnostic(
        title,
        description,
        listOf(solution),
        documentationUrl,
        documentationHint,
        severity,
        throwable
    )
}

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
        severity: ToolingDiagnostic.Severity,
        throwable: Throwable? = null,
    ) : TitleStep, DescriptionStep, SolutionStep, OptionalStep {
        private var state = BuilderState(
            id = id,
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
                checkNotNull(state.id) { "ID is required in diagnostic ${state.title.orEmpty()}" },
                checkNotNull(state.title) { "Title is required for diagnostic with ID ${state.id}" }
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
        severity: ToolingDiagnostic.Severity,
        throwable: Throwable? = null,
        builder: TitleStep.() -> OptionalStep,
    ) = BuilderImpl(id, severity, throwable).apply { builder() }.build()
}