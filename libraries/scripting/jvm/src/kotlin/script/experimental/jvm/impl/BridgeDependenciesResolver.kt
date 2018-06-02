/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvm.impl

import kotlinx.coroutines.experimental.runBlocking
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
import kotlin.script.experimental.jvm.defaultConfiguration
import kotlin.script.experimental.jvm.mapToLegacyScriptReportPosition
import kotlin.script.experimental.jvm.mapToLegacyScriptReportSeverity

class BridgeDependenciesResolver(
    val scriptConfigurator: ScriptCompilationConfigurator?,
    val baseScriptCompilerConfiguration: ScriptCompileConfiguration = scriptConfigurator.defaultConfiguration,
    val onClasspathUpdated: (List<File>) -> Unit = {}
) : AsyncDependenciesResolver {

    override fun resolve(scriptContents: ScriptContents, environment: Environment): DependenciesResolver.ResolveResult =
        runBlocking {
            resolveAsync(scriptContents, environment)
        }

    override suspend fun resolveAsync(scriptContents: ScriptContents, environment: Environment): DependenciesResolver.ResolveResult {
        return try {

            val diagnostics = arrayListOf<ScriptReport>()
            val processedScriptData =
                ProcessedScriptData(ProcessedScriptDataProperties.foundAnnotations to scriptContents.annotations)

            val refinedConfiguration = scriptConfigurator?.let {
                val res = scriptConfigurator.refineConfiguration(
                    scriptContents.toScriptSource(),
                    baseScriptCompilerConfiguration,
                    processedScriptData
                )
                when (res) {
                    is ResultWithDiagnostics.Failure ->
                        return@resolveAsync DependenciesResolver.ResolveResult.Failure(res.reports.mapScriptReportsToDiagnostics())
                    is ResultWithDiagnostics.Success -> {
                        diagnostics.addAll(res.reports.mapScriptReportsToDiagnostics())
                        res.value
                    }
                }
            } ?: baseScriptCompilerConfiguration

            val newClasspath = refinedConfiguration.getOrNull(ScriptCompileConfigurationProperties.dependencies)
                ?.flatMap { (it as JvmDependency).classpath } ?: emptyList()
            if (refinedConfiguration != baseScriptCompilerConfiguration) {
                val oldClasspath = baseScriptCompilerConfiguration.getOrNull(ScriptCompileConfigurationProperties.dependencies)
                    ?.flatMap { (it as JvmDependency).classpath } ?: emptyList()
                if (newClasspath != oldClasspath) {
                    onClasspathUpdated(newClasspath)
                }
            }
            DependenciesResolver.ResolveResult.Success(
                ScriptDependencies(
                    classpath = newClasspath, // TODO: maybe it should return only increment from the initial config
                    imports = refinedConfiguration.getOrNull(ScriptCompileConfigurationProperties.defaultImports)?.toList()
                            ?: emptyList()
                ),
                diagnostics
            )
        } catch (e: Throwable) {
            DependenciesResolver.ResolveResult.Failure(
                ScriptReport(e.message ?: "unknown error $e")
            )
        }
    }
}

internal fun List<ScriptDiagnostic>.mapScriptReportsToDiagnostics() =
    map { ScriptReport(it.message, mapToLegacyScriptReportSeverity(it.severity), mapToLegacyScriptReportPosition(it.location)) }

internal fun ScriptContents.toScriptSource(): ScriptSource = when {
    text != null -> text!!.toString().toScriptSource()
    file != null -> file!!.toScriptSource()
    else -> throw IllegalArgumentException("Unable to convert script contents $this into script source")
}

