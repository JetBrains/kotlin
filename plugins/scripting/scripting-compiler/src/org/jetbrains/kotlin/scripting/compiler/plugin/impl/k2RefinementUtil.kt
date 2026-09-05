/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.impl

import org.jetbrains.kotlin.scripting.resolve.resolvedImportScripts
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.*
import kotlin.script.experimental.impl.refineOnAnnotationsWithLazyDataCollection
import kotlin.script.experimental.jvm.updateClasspath
import kotlin.script.experimental.jvm.util.toClassPathOrEmpty

fun ScriptCompilationConfiguration.refineAllForK2(
    script: SourceCode,
    hostConfiguration: ScriptingHostConfiguration,
    collectAnnotationData: (SourceCode, ScriptCompilationConfiguration) -> ResultWithDiagnostics<ScriptCollectedData>?
): ResultWithDiagnostics<ScriptCompilationConfiguration> =
    with {
        this@with.hostConfiguration.update { it.withDefaultsFrom(hostConfiguration) }
        updateClasspath(hostConfiguration[ScriptingHostConfiguration.configurationDependencies]?.toClassPathOrEmpty())
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
                    return makeFailureResult("Imported source file not found: ${imported.file}".asErrorDiagnostics(path = script.locationId))
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
        }.onSuccess {
            it.checkDependenciesResolved(script)
        }

// `toClassPathOrEmpty` drops non-JvmDependency entries, so leftovers would compile with an empty classpath.
private fun ScriptCompilationConfiguration.checkDependenciesResolved(
    script: SourceCode,
): ResultWithDiagnostics<ScriptCompilationConfiguration> {
    val unresolved = get(ScriptCompilationConfiguration.dependencies)
        ?.filterIsInstance<UnresolvedExternalArtifacts>()
        ?.takeIf { it.isNotEmpty() }
        ?: return asSuccess()

    return ResultWithDiagnostics.Failure(
        unresolved.map {
            ScriptDiagnostic(
                ScriptDiagnostic.unspecifiedError,
                "External artifacts are not resolved: ${it.artifacts.joinToString()}. " +
                        "The scripting host turned off artifacts resolution in the script definition " +
                        "(see ScriptingHostConfiguration.resolveExternalArtifacts) but did not resolve them itself",
                sourcePath = script.locationId,
                location = it.sourceCodeLocation?.locationInText
            )
        }
    )
}
