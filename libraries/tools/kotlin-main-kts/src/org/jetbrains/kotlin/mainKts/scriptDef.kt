/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.mainKts

import org.jetbrains.kotlin.script.util.DependsOn
import org.jetbrains.kotlin.script.util.FilesAndIvyResolver
import org.jetbrains.kotlin.script.util.Repository
import java.io.File
import kotlin.script.dependencies.ScriptContents
import kotlin.script.dependencies.ScriptDependenciesResolver
import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.api.*
import kotlin.script.experimental.jvm.JvmDependency
import kotlin.script.experimental.jvm.compat.mapLegacyDiagnosticSeverity
import kotlin.script.experimental.jvm.compat.mapLegacyScriptPosition
import kotlin.script.experimental.jvm.dependenciesFromClassContext
import kotlin.script.experimental.jvm.dependenciesFromCurrentContext
import kotlin.script.experimental.jvm.jvm

@Suppress("unused")
@KotlinScript(fileExtension = "main.kts", compilationConfiguration = MainKtsScriptDefinition::class)
abstract class MainKtsScript(val args: Array<String>)

object MainKtsScriptDefinition : ScriptCompilationConfiguration(
    {
        defaultImports(DependsOn::class, Repository::class)
        jvm {
            dependenciesFromClassContext(MainKtsScriptDefinition::class, "kotlin-main-kts")
        }
        refineConfiguration {
            onAnnotations(DependsOn::class, Repository::class, handler = MainKtsConfigurator())
        }
    })

class MainKtsConfigurator : RefineScriptCompilationConfigurationHandler {
    private val resolver = FilesAndIvyResolver()

    override operator fun invoke(context: ScriptConfigurationRefinementContext): ResultWithDiagnostics<ScriptCompilationConfiguration> {
        val annotations = context.collectedData?.get(ScriptCollectedData.foundAnnotations)?.takeIf { it.isNotEmpty() }
            ?: return context.compilationConfiguration.asSuccess()
        val scriptContents = object : ScriptContents {
            override val annotations: Iterable<Annotation> = annotations
            override val file: File? = null
            override val text: CharSequence? = null
        }
        val diagnostics = arrayListOf<ScriptDiagnostic>()
        fun report(severity: ScriptDependenciesResolver.ReportSeverity, message: String, position: ScriptContents.Position?) {
            diagnostics.add(ScriptDiagnostic(message, mapLegacyDiagnosticSeverity(severity), mapLegacyScriptPosition(position)))
        }
        return try {
            val newDepsFromResolver = resolver.resolve(scriptContents, emptyMap(), ::report, null).get()
                ?: return context.compilationConfiguration.asSuccess(diagnostics) // TODO: failure
            val resolvedClasspath = newDepsFromResolver.classpath.toList().takeIf { it.isNotEmpty() }
                ?: return context.compilationConfiguration.asSuccess(diagnostics) // TODO: failure

            ScriptCompilationConfiguration(context.compilationConfiguration) {
                dependencies.append(JvmDependency(resolvedClasspath))
            }.asSuccess(diagnostics)
        } catch (e: Throwable) {
            ResultWithDiagnostics.Failure(*diagnostics.toTypedArray(), e.asDiagnostics())
        }
    }
}

