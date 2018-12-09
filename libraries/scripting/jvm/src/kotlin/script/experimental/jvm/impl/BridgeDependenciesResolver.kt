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
import kotlin.script.experimental.host.FileScriptSource
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
                scriptCompilationConfiguration[ScriptCompilationConfiguration.dependencies].toClassPathOrEmpty()

            val defaultImports = scriptCompilationConfiguration[ScriptCompilationConfiguration.defaultImports]?.toList() ?: emptyList()

            fun ScriptCompilationConfiguration.toDependencies(classpath: List<File>): ScriptDependencies =
                ScriptDependencies(
                    classpath = classpath,
                    sources = this[ScriptCompilationConfiguration.ide.dependenciesSources].toClassPathOrEmpty(),
                    imports = defaultImports,
                    scripts = this[ScriptCompilationConfiguration.importScripts].toFilesOrEmpty()
                )

            val refineResults = scriptCompilationConfiguration.refineWith(
                scriptCompilationConfiguration[ScriptCompilationConfiguration.refineConfigurationOnAnnotations]?.handler,
                scriptContents, processedScriptData
            ).onSuccess {
                it.refineWith(
                    scriptCompilationConfiguration[ScriptCompilationConfiguration.refineConfigurationBeforeCompiling]?.handler,
                    scriptContents, processedScriptData
                )
            }

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
                // TODO: consider returning only increment from the initial config
                refinedConfiguration.toDependencies(newClasspath),
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
    file != null -> FileScriptSource(file!!, text?.toString())
    text != null -> text!!.toString().toScriptSource()
    else -> throw IllegalArgumentException("Unable to convert script contents $this into script source")
}

internal fun List<ScriptDependency>?.toClassPathOrEmpty() = this?.flatMap { (it as JvmDependency).classpath } ?: emptyList()

internal fun List<SourceCode>?.toFilesOrEmpty() = this?.map {
    val externalSource = it as? ExternalSourceCode
    externalSource?.externalLocation?.toFile()
        ?: throw RuntimeException("Unsupported source in requireSources parameter - only local files are supported now (${externalSource?.externalLocation})")
} ?: emptyList()

fun ScriptCompilationConfiguration.refineWith(
    handler: RefineScriptCompilationConfigurationHandler?, scriptContents: ScriptContents, processedScriptData: ScriptCollectedData
): ResultWithDiagnostics<ScriptCompilationConfiguration> {

    if (handler == null) return this.asSuccess()

    return handler(
        ScriptConfigurationRefinementContext(scriptContents.toScriptSource(), this, processedScriptData)
    )
}