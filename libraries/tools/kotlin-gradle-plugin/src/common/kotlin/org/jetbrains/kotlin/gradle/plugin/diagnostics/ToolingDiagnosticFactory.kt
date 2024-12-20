/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics

import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi

@InternalKotlinGradlePluginApi // used in integration tests
abstract class ToolingDiagnosticFactory(
    private val predefinedSeverity: ToolingDiagnostic.Severity? = null,
    customId: String? = null,
) {
    open val id: String = customId ?: this::class.simpleName!!

    protected fun build(
        severity: ToolingDiagnostic.Severity? = null,
        throwable: Throwable? = null,
        builder: ToolingDiagnosticBuilder.() -> Unit,
    ) = ToolingDiagnosticBuilderImpl().apply(builder).let { diagnosticBuilder ->
        val finalSeverity = severity ?: predefinedSeverity ?: error(
            "Can't determine severity. Either provide it in constructor of ToolingDiagnosticFactory," +
                    " or in the 'build'-function invocation"
        )

        if (severity != null && predefinedSeverity != null) {
            error(
                "Please provide severity either in ToolingDiagnosticFactory constructor, or as the 'build'-function parameter," +
                        " but not both at once"
            )
        }

        ToolingDiagnostic(
            identifier = ToolingDiagnostic.ID(id, diagnosticBuilder.name),
            message = diagnosticBuilder.message,
            severity = finalSeverity,
            solutions = diagnosticBuilder.solutions,
            documentation = diagnosticBuilder.documentation,
            throwable = throwable
        )
    }
}

@InternalKotlinGradlePluginApi
interface ToolingDiagnosticBuilder {
    fun name(string: () -> String)
    fun message(string: () -> String)
    fun solution(singleString: () -> String)
    fun solutions(stringList: () -> List<String>)
    fun documentation(url: String, urlBuilder: (String) -> String = { "See $url for more details." })
}

private class ToolingDiagnosticBuilderImpl : ToolingDiagnosticBuilder {

    val name: String get() = _name ?: error("Name is not provided")
    val message: String get() = _message ?: error("Message is not provided")
    val solutions: List<String> get() = _solutions.toList()
    val documentation: ToolingDiagnostic.Documentation? get() = _documentation

    private var _name: String? = null
    private var _message: String? = null
    private var _solutions: MutableList<String> = mutableListOf()
    private var _documentation: ToolingDiagnostic.Documentation? = null

    private fun checkSolutionIsSingleLine(text: String) {
        require(text.lines().size == 1) { "Solution should not be multi-line: $text" }
    }

    override fun name(string: () -> String) {
        _name = string()
    }

    override fun message(string: () -> String) {
        _message = string()
    }

    override fun solution(singleString: () -> String) {
        singleString().takeIf { it.isNotBlank() }?.let {
            checkSolutionIsSingleLine(it)
            _solutions.add(it)
        }
    }

    override fun solutions(stringList: () -> List<String>) {
        stringList().filter { it.isNotBlank() }.let { strings ->
            strings.forEach(::checkSolutionIsSingleLine)
            _solutions.addAll(strings)
        }
    }

    override fun documentation(url: String, urlBuilder: (String) -> String) {
        _documentation = ToolingDiagnostic.Documentation(url, urlBuilder(url))
    }
}