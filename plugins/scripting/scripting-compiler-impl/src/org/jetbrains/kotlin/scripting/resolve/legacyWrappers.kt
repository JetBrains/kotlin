/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DEPRECATION")

package org.jetbrains.kotlin.scripting.resolve

import java.io.File
import kotlin.script.dependencies.Environment
import kotlin.script.dependencies.ScriptDependenciesResolver
import kotlin.script.experimental.dependencies.DependenciesResolver
import kotlin.script.experimental.dependencies.ScriptDependencies
import kotlin.script.experimental.dependencies.ScriptReport

interface LegacyResolverWrapper

class ApiChangeDependencyResolverWrapper(
        override val delegate: kotlin.script.dependencies.ScriptDependenciesResolver
) : kotlin.script.experimental.dependencies.DependenciesResolver,
    DependencyResolverWrapper<ScriptDependenciesResolver>,
    LegacyResolverWrapper {

    private var previousDependencies: kotlin.script.dependencies.KotlinScriptExternalDependencies? = null

    override fun resolve(
            scriptContents: kotlin.script.dependencies.ScriptContents,
            environment: Environment
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

    private fun kotlin.script.dependencies.ScriptDependenciesResolver.ReportSeverity.convertSeverity(): ScriptReport.Severity = when (this) {
        kotlin.script.dependencies.ScriptDependenciesResolver.ReportSeverity.FATAL -> ScriptReport.Severity.FATAL
        kotlin.script.dependencies.ScriptDependenciesResolver.ReportSeverity.ERROR -> ScriptReport.Severity.ERROR
        kotlin.script.dependencies.ScriptDependenciesResolver.ReportSeverity.WARNING -> ScriptReport.Severity.WARNING
        kotlin.script.dependencies.ScriptDependenciesResolver.ReportSeverity.INFO -> ScriptReport.Severity.INFO
        kotlin.script.dependencies.ScriptDependenciesResolver.ReportSeverity.DEBUG -> ScriptReport.Severity.DEBUG
    }

    private fun kotlin.script.dependencies.ScriptContents.Position.convertPosition(): ScriptReport.Position = ScriptReport.Position(line, col)
}