/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.mainKts

import org.jetbrains.kotlin.mainKts.impl.FilesAndIvyResolver
import org.jetbrains.kotlin.script.util.DependsOn
import org.jetbrains.kotlin.script.util.Import
import org.jetbrains.kotlin.script.util.Repository
import java.io.File
import kotlin.script.dependencies.ScriptContents
import kotlin.script.dependencies.ScriptDependenciesResolver
import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.FileScriptSource
import kotlin.script.experimental.jvm.compat.mapLegacyDiagnosticSeverity
import kotlin.script.experimental.jvm.compat.mapLegacyScriptPosition
import kotlin.script.experimental.jvm.dependenciesFromClassContext
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvm.updateClasspath

@Suppress("unused")
@KotlinScript(fileExtension = "main.kts", compilationConfiguration = MainKtsScriptDefinition::class)
abstract class MainKtsScript(val args: Array<String>)

object MainKtsScriptDefinition : ScriptCompilationConfiguration(
    {
        defaultImports(DependsOn::class, Repository::class, Import::class)
        jvm {
            dependenciesFromClassContext(MainKtsScriptDefinition::class, "kotlin-main-kts", "kotlin-stdlib", "kotlin-reflect")
        }
        refineConfiguration {
            onAnnotations(DependsOn::class, Repository::class, Import::class, handler = MainKtsConfigurator())
        }
        ide {
            acceptedLocations(ScriptAcceptedLocation.Everywhere)
        }
    })

class MainKtsConfigurator : RefineScriptCompilationConfigurationHandler {
    private val resolver = FilesAndIvyResolver()

    override operator fun invoke(context: ScriptConfigurationRefinementContext): ResultWithDiagnostics<ScriptCompilationConfiguration> {
        val diagnostics = arrayListOf<ScriptDiagnostic>()

        fun report(severity: ScriptDependenciesResolver.ReportSeverity, message: String, position: ScriptContents.Position?) {
            diagnostics.add(
                ScriptDiagnostic(
                    message,
                    mapLegacyDiagnosticSeverity(severity),
                    context.script.locationId,
                    mapLegacyScriptPosition(position)
                )
            )
        }

        val annotations = context.collectedData?.get(ScriptCollectedData.foundAnnotations)?.takeIf { it.isNotEmpty() }
            ?: return context.compilationConfiguration.asSuccess()

        val scriptBaseDir = (context.script as? FileScriptSource)?.file?.parentFile
        val importedSources = annotations.flatMap {
            (it as? Import)?.paths?.map { sourceName ->
                FileScriptSource(scriptBaseDir?.resolve(sourceName) ?: File(sourceName))
            } ?: emptyList()
        }

        val resolvedClassPath = try {
            val scriptContents = object : ScriptContents {
                override val annotations: Iterable<Annotation> = annotations.filter { it is DependsOn || it is Repository }
                override val file: File? = null
                override val text: CharSequence? = null
            }
            resolver.resolve(scriptContents, emptyMap(), ::report, null).get()?.classpath?.toList()
            // TODO: add diagnostics
        } catch (e: Throwable) {
            return ResultWithDiagnostics.Failure(*diagnostics.toTypedArray(), e.asDiagnostics(path = context.script.locationId))
        }

        return ScriptCompilationConfiguration(context.compilationConfiguration) {
            if (resolvedClassPath != null) updateClasspath(resolvedClassPath)
            if (importedSources.isNotEmpty()) importScripts.append(importedSources)
        }.asSuccess(diagnostics)
    }
}

