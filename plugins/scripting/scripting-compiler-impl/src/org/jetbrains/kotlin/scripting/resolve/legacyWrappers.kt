/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DEPRECATION")

package org.jetbrains.kotlin.scripting.resolve

import org.jetbrains.kotlin.scripting.definitions.DependencyResolverWrapper
import java.io.File
import kotlin.script.dependencies.Environment
import kotlin.script.dependencies.ScriptDependenciesResolver
import kotlin.script.experimental.dependencies.DependenciesResolver
import kotlin.script.experimental.dependencies.ScriptDependencies
import kotlin.script.experimental.dependencies.ScriptReport

internal class ApiChangeDependencyResolverWrapper(
    override val delegate: ScriptDependenciesResolver,
) : DependenciesResolver,
    DependencyResolverWrapper<ScriptDependenciesResolver> {

    private var previousDependencies: kotlin.script.dependencies.KotlinScriptExternalDependencies? = null

    override fun resolve(
        scriptContents: kotlin.script.dependencies.ScriptContents,
        environment: Environment,
    ): DependenciesResolver.ResolveResult {
        val reports = ArrayList<ScriptReport>()
        val legacyDeps = delegate.resolve(
            scriptContents,
            environment,
            { sev, msg, pos ->
                reports.add(ScriptReport(msg, sev.convertSeverity(), pos?.convertPosition()))
            },
            previousDependencies
        ).get() ?: return DependenciesResolver.ResolveResult.Failure(reports)

        val dependencies = ScriptDependencies(
            javaHome = legacyDeps.javaHome?.let(::File),
            classpath = legacyDeps.classpath.toList(),
            imports = legacyDeps.imports.toList(),
            sources = legacyDeps.sources.toList(),
            scripts = legacyDeps.scripts.toList()
        )
        previousDependencies = legacyDeps
        return DependenciesResolver.ResolveResult.Success(dependencies, reports)
    }

    private fun ScriptDependenciesResolver.ReportSeverity.convertSeverity(): ScriptReport.Severity = when (this) {
        ScriptDependenciesResolver.ReportSeverity.FATAL -> ScriptReport.Severity.FATAL
        ScriptDependenciesResolver.ReportSeverity.ERROR -> ScriptReport.Severity.ERROR
        ScriptDependenciesResolver.ReportSeverity.WARNING -> ScriptReport.Severity.WARNING
        ScriptDependenciesResolver.ReportSeverity.INFO -> ScriptReport.Severity.INFO
        ScriptDependenciesResolver.ReportSeverity.DEBUG -> ScriptReport.Severity.DEBUG
    }

    private fun kotlin.script.dependencies.ScriptContents.Position.convertPosition(): ScriptReport.Position =
        ScriptReport.Position(line, col)
}