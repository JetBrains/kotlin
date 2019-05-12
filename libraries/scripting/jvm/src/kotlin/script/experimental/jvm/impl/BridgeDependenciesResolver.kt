/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
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
    val onConfigurationUpdated: (SourceCode, ScriptCompilationConfiguration) -> Unit = { _, _ -> },
    val getScriptSource: (ScriptContents) -> SourceCode? = { null }
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

            val script = getScriptSource(scriptContents) ?: scriptContents.toScriptSource()

            val refineResults = scriptCompilationConfiguration.refineWith(
                scriptCompilationConfiguration[ScriptCompilationConfiguration.refineConfigurationOnAnnotations]?.handler,
                processedScriptData, script
            ).onSuccess {
                it.refineWith(
                    scriptCompilationConfiguration[ScriptCompilationConfiguration.refineConfigurationBeforeCompiling]?.handler,
                    processedScriptData, script
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

            if (refinedConfiguration != scriptCompilationConfiguration) {
                onConfigurationUpdated(script, refinedConfiguration)
            }

            val newClasspath = refinedConfiguration[ScriptCompilationConfiguration.dependencies]
                ?.flatMap { (it as JvmDependency).classpath } ?: emptyList()

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

fun ScriptCompilationConfiguration.toDependencies(classpath: List<File>): ScriptDependencies {
    val defaultImports = this[ScriptCompilationConfiguration.defaultImports]?.toList() ?: emptyList()

    return ScriptDependencies(
        classpath = classpath,
        sources = this[ScriptCompilationConfiguration.ide.dependenciesSources].toClassPathOrEmpty(),
        imports = defaultImports,
        scripts = this[ScriptCompilationConfiguration.importScripts].toFilesOrEmpty()
    )
}

internal fun List<ScriptDiagnostic>.mapScriptReportsToDiagnostics() =
    map { ScriptReport(it.message, mapToLegacyScriptReportSeverity(it.severity), mapToLegacyScriptReportPosition(it.location)) }

internal fun ScriptContents.toScriptSource(): SourceCode = when {
    file != null -> FileScriptSource(file!!, text?.toString())
    text != null -> text!!.toString().toScriptSource()
    else -> throw IllegalArgumentException("Unable to convert script contents $this into script source")
}

fun List<ScriptDependency>?.toClassPathOrEmpty() = this?.flatMap { (it as JvmDependency).classpath } ?: emptyList()

internal fun List<SourceCode>?.toFilesOrEmpty() = this?.map {
    val externalSource = it as? ExternalSourceCode
    externalSource?.externalLocation?.toFileOrNull()
        ?: throw RuntimeException("Unsupported source in requireSources parameter - only local files are supported now (${externalSource?.externalLocation})")
} ?: emptyList()

fun ScriptCompilationConfiguration.refineWith(
    handler: RefineScriptCompilationConfigurationHandler?,
    processedScriptData: ScriptCollectedData,
    script: SourceCode
): ResultWithDiagnostics<ScriptCompilationConfiguration> =
    if (handler == null) this.asSuccess()
    else handler(ScriptConfigurationRefinementContext(script, this, processedScriptData))