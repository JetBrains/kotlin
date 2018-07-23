/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.script.examples.jvm.resolve.maven

import org.jetbrains.kotlin.script.util.DependsOn
import org.jetbrains.kotlin.script.util.FilesAndMavenResolver
import org.jetbrains.kotlin.script.util.Repository
import java.io.File
import kotlin.script.dependencies.ScriptContents
import kotlin.script.dependencies.ScriptDependenciesResolver
import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.api.*
import kotlin.script.experimental.jvm.JvmDependency
import kotlin.script.experimental.jvm.compat.mapLegacyDiagnosticSeverity
import kotlin.script.experimental.jvm.compat.mapLegacyScriptPosition
import kotlin.script.experimental.jvm.jvmDependenciesFromCurrentContext
import kotlin.script.experimental.misc.invoke

@KotlinScript(
    extension = "scriptwithdeps.kts",
    properties = MyScriptProperties::class
)
abstract class MyScriptWithMavenDeps {
//    abstract fun body(vararg args: String): Int
}

object MyScriptProperties : ScriptingProperties() {
    override fun setup() {
        scriptDefinition {
            defaultImports<DependsOn>()
            defaultImports(Repository::class)
            jvmDependenciesFromCurrentContext(
                "scripting-jvm-maven-deps", // script library jar name
                "kotlin-script-util" // DependsOn annotation is taken from script-util
            )
            // variant: dependencies(collectDependenciesFromCurrentContext(...
            refineConfiguration {
            // variant ^: dynamicConfiguration
                handler(MyConfigurator())
                triggerOnAnnotations(DependsOn::class, Repository::class)
                // variants: onAnnotations, refineOnAnnotations (esp. for dynamicConfiguration), updateOnAnnotations
                // other triggers: beforeParsing, onSections
            }
        }
    }
}

class MyConfigurator : RefineScriptCompilationConfigurationHandler {

    private val resolver = FilesAndMavenResolver()

    override suspend operator fun invoke(
        scriptSource: ScriptSource,
        configuration: ScriptCompileConfiguration,
        processedScriptData: ProcessedScriptData
    ): ResultWithDiagnostics<ScriptCompileConfiguration> {
        val annotations = processedScriptData.getOrNull(ProcessedScriptDataProperties.foundAnnotations)?.takeIf { it.isNotEmpty() }
            ?: return configuration.asSuccess()
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
                ?: return configuration.asSuccess(diagnostics)
            val resolvedClasspath = newDepsFromResolver.classpath.toList().takeIf { it.isNotEmpty() }
                ?: return configuration.asSuccess(diagnostics)
            val newDependency = JvmDependency(resolvedClasspath)
            val updatedDeps =
                configuration.getOrNull(ScriptDefinitionProperties.dependencies)?.plus(newDependency) ?: listOf(newDependency)
            ScriptCompileConfiguration(configuration, ScriptDefinitionProperties.dependencies(updatedDeps)).asSuccess(diagnostics)
        } catch (e: Throwable) {
            ResultWithDiagnostics.Failure(*diagnostics.toTypedArray(), e.asDiagnostics())
        }
    }
}

