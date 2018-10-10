/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvm.impl

import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.script.dependencies.Environment
import kotlin.script.dependencies.ScriptContents
import kotlin.script.experimental.api.*
import kotlin.script.experimental.dependencies.AsyncDependenciesResolver
import kotlin.script.experimental.dependencies.DependenciesResolver
import kotlin.script.experimental.dependencies.ScriptDependencies
import kotlin.script.experimental.dependencies.ScriptReport
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.JvmDependency
import kotlin.script.experimental.jvm.compat.mapToLegacyScriptReportPosition
import kotlin.script.experimental.jvm.compat.mapToLegacyScriptReportSeverity

class BridgeDependenciesResolver(
    val scriptCompilationConfiguration: ScriptCompilationConfiguration,
    val onClasspathUpdated: (List<File>) -> Unit = {}
) : AsyncDependenciesResolver {

    override fun resolve(scriptContents: ScriptContents, environment: Environment): DependenciesResolver.ResolveResult =
        runBlocking {
            resolveAsync(scriptContents, environment)
        }

    override suspend fun resolveAsync(scriptContents: ScriptContents, environment: Environment): DependenciesResolver.ResolveResult {
        try {

            val diagnostics = arrayListOf<ScriptReport>()
            val processedScriptData = ScriptCollectedData(
                mapOf(
                    ScriptCollectedData.foundAnnotations to scriptContents.annotations
                )
            )

            val oldClasspath =
                scriptCompilationConfiguration[ScriptCompilationConfiguration.dependencies]
                    ?.flatMap { (it as JvmDependency).classpath } ?: emptyList()

            val defaultImports = scriptCompilationConfiguration[ScriptCompilationConfiguration.defaultImports]?.toList() ?: emptyList()

            val refineFn = scriptCompilationConfiguration[ScriptCompilationConfiguration.refineConfigurationOnAnnotations]?.handler
                ?: return DependenciesResolver.ResolveResult.Success(
                    ScriptDependencies(classpath = oldClasspath, imports = defaultImports),
                    diagnostics
                )

            val refineResults = refineFn(
                ScriptConfigurationRefinementContext(scriptContents.toScriptSource(), scriptCompilationConfiguration, processedScriptData)
            )
            val refinedConfiguration = when (refineResults) {
                is ResultWithDiagnostics.Failure ->
                    return DependenciesResolver.ResolveResult.Failure(refineResults.reports.mapScriptReportsToDiagnostics())
                is ResultWithDiagnostics.Success -> {
                    diagnostics.addAll(refineResults.reports.mapScriptReportsToDiagnostics())
                    refineResults.value
                }
            }

            val newClasspath = refinedConfiguration[ScriptCompilationConfiguration.dependencies]
                ?.flatMap { (it as JvmDependency).classpath } ?: emptyList()
            if (newClasspath != oldClasspath) {
                onClasspathUpdated(newClasspath)
            }

            return DependenciesResolver.ResolveResult.Success(
                ScriptDependencies(
                    classpath = newClasspath, // TODO: maybe it should return only increment from the initial config
                    imports = defaultImports
                ),
                diagnostics
            )
        } catch (e: Throwable) {
            return DependenciesResolver.ResolveResult.Failure(
                ScriptReport(e.message ?: "unknown error $e")
            )
        }
    }
}

internal fun List<ScriptDiagnostic>.mapScriptReportsToDiagnostics() =
    map { ScriptReport(it.message, mapToLegacyScriptReportSeverity(it.severity), mapToLegacyScriptReportPosition(it.location)) }

internal fun ScriptContents.toScriptSource(): SourceCode = when {
    text != null -> text!!.toString().toScriptSource()
    file != null -> file!!.toScriptSource()
    else -> throw IllegalArgumentException("Unable to convert script contents $this into script source")
}

