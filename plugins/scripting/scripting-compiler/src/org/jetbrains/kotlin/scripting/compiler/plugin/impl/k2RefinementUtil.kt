/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.impl

import org.jetbrains.kotlin.diagnostics.impl.BaseDiagnosticsCollector
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.scripting.resolve.InvalidScriptResolverAnnotation
import org.jetbrains.kotlin.scripting.resolve.KtFileScriptSource
import org.jetbrains.kotlin.scripting.resolve.resolvedImportScripts
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.FileBasedScriptSource
import kotlin.script.experimental.host.FileScriptSource
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.host.configurationDependencies
import kotlin.script.experimental.host.withDefaultsFrom
import kotlin.script.experimental.impl.refineOnAnnotationsWithLazyDataCollection
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration
import kotlin.script.experimental.jvm.updateClasspath
import kotlin.script.experimental.jvm.util.toClassPathOrEmpty

internal fun effectiveHostConfiguration(
    explicit: ScriptingHostConfiguration,
    configuration: ScriptCompilationConfiguration,
): ScriptingHostConfiguration =
    explicit.withDefaultsFrom(
        configuration[ScriptCompilationConfiguration.hostConfiguration] ?: defaultJvmScriptingHostConfiguration
    )

fun ScriptCompilationConfiguration.refineAllForK2(
    script: SourceCode,
    hostConfiguration: ScriptingHostConfiguration,
    collectAnnotationData: (SourceCode, ScriptCompilationConfiguration) -> ResultWithDiagnostics<ScriptCollectedData>?
): ResultWithDiagnostics<ScriptCompilationConfiguration> =
    effectiveHostConfiguration(hostConfiguration, this).let { effectiveHostConfig ->
        with {
            this@with.hostConfiguration(effectiveHostConfig)
            updateClasspath(effectiveHostConfig[ScriptingHostConfiguration.configurationDependencies]?.toClassPathOrEmpty())
        }
    }
        .refineBeforeParsing(script)
        .onSuccess {
            it.refineOnAnnotationsWithLazyDataCollection(script) {
                collectAnnotationData(script, it)
            }
        }.onSuccess {
            it.refineBeforeCompiling(script)
        }.onSuccess {
            val resolvedScripts = it[ScriptCompilationConfiguration.importScripts]?.map { imported ->
                if (imported is FileBasedScriptSource && !imported.file.exists())
                    return@onSuccess makeFailureResult(
                        "Imported source file not found: ${imported.file}".asErrorDiagnostics(path = script.locationId)
                    )
                when (imported) {
                    is FileScriptSource -> {
                        val absoluteFile = imported.file.normalize().absoluteFile
                        if (imported.file == absoluteFile) imported else FileScriptSource(absoluteFile)
                    }
                    else -> imported
                }
            }
            if (resolvedScripts.isNullOrEmpty()) it.asSuccess()
            else it.with {
                resolvedImportScripts(resolvedScripts)
            }.asSuccess()
        }

/**
 * Provides the session in which the file annotations of the script are resolved, see [collectAndResolveScriptAnnotationsViaFir].
 * It is the place where the Analysis API is expected to supply its own session (KT-89684).
 */
internal typealias AnnotationResolutionSessionProvider = (SourceCode, ScriptCompilationConfiguration) -> FirSession

internal fun SourceCode.defaultFirConverter(): SourceCode.(FirSession, BaseDiagnosticsCollector) -> FirFile =
    if (this is KtFileScriptSource) SourceCode::convertToFirViaPsi else SourceCode::convertToFirViaLightTree

/**
 * [refineAllForK2] with the annotations collected by [collectAndResolveScriptAnnotationsViaFir]. The accepted annotations that cannot be
 * constructed are passed to the refinement handlers as [InvalidScriptResolverAnnotation]s. If the refinement succeeds nevertheless,
 * they are reported as errors in the [failOnInvalidAnnotations] (compiler) mode, and as warnings otherwise (IDE).
 */
internal fun ScriptCompilationConfiguration.refineAllViaFir(
    script: SourceCode,
    hostConfiguration: ScriptingHostConfiguration,
    getAnnotationSession: AnnotationResolutionSessionProvider,
    convertToFir: SourceCode.(FirSession, BaseDiagnosticsCollector) -> FirFile = script.defaultFirConverter(),
    failOnInvalidAnnotations: Boolean = true,
): ResultWithDiagnostics<ScriptCompilationConfiguration> {
    var invalidAnnotations: List<ScriptSourceAnnotation<*>> = emptyList()
    val result = refineAllForK2(script, hostConfiguration) { source, configuration ->
        collectAndResolveScriptAnnotationsViaFir(source, configuration, hostConfiguration, getAnnotationSession, convertToFir).also {
            invalidAnnotations = it.valueOrNull()?.get(ScriptCollectedData.collectedAnnotations).orEmpty()
                .filter { annotation -> annotation.annotation is InvalidScriptResolverAnnotation }
        }
    }
    if (!failOnInvalidAnnotations || result !is ResultWithDiagnostics.Success || invalidAnnotations.isEmpty()) return result
    val errors = invalidAnnotations.map {
        ScriptDiagnostic(
            ScriptDiagnostic.unspecifiedError,
            (it.annotation as InvalidScriptResolverAnnotation).diagnosticMessage(),
            ScriptDiagnostic.Severity.ERROR,
            it.location?.codeLocationId ?: script.locationId,
            it.location?.locationInText,
        )
    }
    // the same messages were reported as warnings by the annotations collecting
    val errorMessages = errors.mapTo(HashSet()) { it.message }
    return ResultWithDiagnostics.Failure(result.reports.filterNot { it.message in errorMessages } + errors)
}
