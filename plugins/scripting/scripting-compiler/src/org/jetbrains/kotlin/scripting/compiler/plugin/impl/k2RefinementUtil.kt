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
import kotlin.script.experimental.host.getScriptingClass
import kotlin.script.experimental.host.with
import kotlin.script.experimental.host.withDefaultsFrom
import kotlin.script.experimental.impl.refineOnAnnotationsWithLazyDataCollection
import kotlin.script.experimental.jvm.baseClassLoader
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvm.updateClasspath
import kotlin.script.experimental.jvm.util.toClassPathOrEmpty

/**
 * [explicit] is the host's actual configuration, so it wins over the one from [configuration],
 * except for the keys describing where the definition's classes are loaded from.
 */
internal fun effectiveHostConfiguration(
    explicit: ScriptingHostConfiguration,
    configuration: ScriptCompilationConfiguration,
): ScriptingHostConfiguration {
    val fromConfiguration = configuration[ScriptCompilationConfiguration.hostConfiguration]
        ?: return explicit.withDefaultsFrom(defaultJvmScriptingHostConfiguration)
    return explicit.withDefaultsFrom(fromConfiguration).with {
        fromConfiguration.getNoDefault(ScriptingHostConfiguration.configurationDependencies)?.let { configurationDependencies(it) }
        fromConfiguration.getNoDefault(ScriptingHostConfiguration.jvm.baseClassLoader)?.let { jvm.baseClassLoader(it) }
        fromConfiguration.getNoDefault(ScriptingHostConfiguration.getScriptingClass)?.let { getScriptingClass(it) }
    }
}

/**
 * Provides the session in which the file annotations of the script are resolved, see [collectAndResolveScriptAnnotationsViaFir].
 * It is the place where the Analysis API is expected to supply its own session (KT-89684).
 */
internal typealias AnnotationResolutionSessionProvider = (SourceCode, ScriptCompilationConfiguration) -> FirSession

internal fun SourceCode.defaultFirConverter(): SourceCode.(FirSession, BaseDiagnosticsCollector) -> FirFile =
    if (this is KtFileScriptSource) SourceCode::convertToFirViaPsi else SourceCode::convertToFirViaLightTree

/**
 * Runs all refinement stages; the annotations are resolved in the session from [getAnnotationSession]. The accepted annotations that
 * cannot be constructed are passed to the refinement handlers as [InvalidScriptResolverAnnotation]s. If the refinement succeeds
 * nevertheless, they are reported as errors in the [failOnInvalidAnnotations] (compiler) mode, and as warnings otherwise (IDE).
 */
internal fun ScriptCompilationConfiguration.refineAllWithFirAnnotationSession(
    script: SourceCode,
    hostConfiguration: ScriptingHostConfiguration,
    getAnnotationSession: AnnotationResolutionSessionProvider,
    convertToFir: SourceCode.(FirSession, BaseDiagnosticsCollector) -> FirFile = script.defaultFirConverter(),
    failOnInvalidAnnotations: Boolean = true,
): ResultWithDiagnostics<ScriptCompilationConfiguration> {
    val invalidAnnotations = mutableListOf<ScriptSourceAnnotation<*>>()
    val effectiveHostConfig = effectiveHostConfiguration(hostConfiguration, this)
    val result = with {
        this@with.hostConfiguration(effectiveHostConfig)
        updateClasspath(effectiveHostConfig[ScriptingHostConfiguration.configurationDependencies]?.toClassPathOrEmpty())
    }
        .refineBeforeParsing(script)
        .onSuccess {
            it.refineOnAnnotationsWithLazyDataCollection(script) {
                collectAndResolveScriptAnnotationsViaFir(
                    script, it, hostConfiguration, getAnnotationSession, convertToFir, reportInvalidAnnotations = false
                ).also { collected ->
                    invalidAnnotations += collected.valueOrNull()?.get(ScriptCollectedData.collectedAnnotations).orEmpty()
                        .filter { annotation -> annotation.annotation is InvalidScriptResolverAnnotation }
                }
            }
        }.onSuccess {
            it.refineBeforeCompiling(script)
        }.onSuccess {
            it.withResolvedImportScripts(script)
        }
    if (invalidAnnotations.isEmpty()) return result
    val failCompilation = failOnInvalidAnnotations && result is ResultWithDiagnostics.Success
    val invalidAnnotationDiagnostics = invalidAnnotations.map {
        ScriptDiagnostic(
            ScriptDiagnostic.unspecifiedError,
            (it.annotation as InvalidScriptResolverAnnotation).diagnosticMessage(),
            if (failCompilation) ScriptDiagnostic.Severity.ERROR else ScriptDiagnostic.Severity.WARNING,
            it.location?.codeLocationId ?: script.locationId,
            it.location?.locationInText,
        )
    }
    return when (result) {
        is ResultWithDiagnostics.Success -> if (failCompilation) {
            ResultWithDiagnostics.Failure(*(result.reports + invalidAnnotationDiagnostics).toTypedArray())
        } else {
            ResultWithDiagnostics.Success(result.value, result.reports + invalidAnnotationDiagnostics)
        }
        is ResultWithDiagnostics.Failure -> ResultWithDiagnostics.Failure(*(result.reports + invalidAnnotationDiagnostics).toTypedArray())
    }
}

private fun ScriptCompilationConfiguration.withResolvedImportScripts(
    script: SourceCode,
): ResultWithDiagnostics<ScriptCompilationConfiguration> {
    val resolvedScripts = this[ScriptCompilationConfiguration.importScripts]?.map { imported ->
        if (imported is FileBasedScriptSource && !imported.file.exists())
            return makeFailureResult("Imported source file not found: ${imported.file}".asErrorDiagnostics(path = script.locationId))
        when (imported) {
            is FileScriptSource -> {
                val absoluteFile = imported.file.normalize().absoluteFile
                if (imported.file == absoluteFile) imported else FileScriptSource(absoluteFile)
            }
            else -> imported
        }
    }
    return if (resolvedScripts.isNullOrEmpty()) asSuccess()
    else with { resolvedImportScripts(resolvedScripts) }.asSuccess()
}
