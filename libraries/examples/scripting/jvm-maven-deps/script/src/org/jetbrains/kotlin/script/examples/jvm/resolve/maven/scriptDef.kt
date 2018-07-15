/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.script.examples.jvm.resolve.maven

import org.jetbrains.kotlin.script.util.DependsOn
import org.jetbrains.kotlin.script.util.FilesAndMavenResolver
import org.jetbrains.kotlin.script.util.Repository
import org.jetbrains.kotlin.script.util.scriptCompilationClasspathFromContext
import java.io.File
import kotlin.script.dependencies.ScriptContents
import kotlin.script.dependencies.ScriptDependenciesResolver
import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.annotations.KotlinScriptDefaultCompilationConfiguration
import kotlin.script.experimental.annotations.KotlinScriptEvaluator
import kotlin.script.experimental.annotations.KotlinScriptFileExtension
import kotlin.script.experimental.api.*
import kotlin.script.experimental.jvm.*
import kotlin.script.experimental.jvm.runners.BasicJvmScriptEvaluator
import kotlin.script.experimental.misc.*
import kotlin.script.experimental.util.TypedKey

@KotlinScript
@KotlinScriptFileExtension("scriptwithdeps.kts")
@KotlinScriptDefaultCompilationConfiguration(MyConfiguration::class)
@KotlinScriptEvaluator(BasicJvmScriptEvaluator::class)
abstract class MyScriptWithMavenDeps {
//    abstract fun body(vararg args: String): Int
}

object MyConfiguration : ArrayList<Pair<TypedKey<*>, Any?>>(
    jvmJavaHomeParams + with(ScriptCompileConfigurationProperties) {
        listOf(
            baseClass<MyScriptWithMavenDeps>(),
            defaultImports(DependsOn::class.qualifiedName!!, Repository::class.qualifiedName!!),
            dependencies(
                JvmDependency(
                    scriptCompilationClasspathFromContext(
                        "scripting-jvm-maven-deps", // script library jar name
                        "kotlin-script-util" // DependsOn annotation is taken from script-util
                    )
                )
            ),
            refineConfiguration(MyConfigurator()),
            refineConfigurationOnAnnotations(DependsOn::class, Repository::class)
        )
    }
)

class MyConfigurator : RefineScriptCompilationConfiguration {

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
                configuration.getOrNull(ScriptCompileConfigurationProperties.dependencies)?.plus(newDependency) ?: listOf(newDependency)
            ScriptCompileConfiguration(configuration, ScriptCompileConfigurationProperties.dependencies(updatedDeps)).asSuccess(diagnostics)
        } catch (e: Throwable) {
            ResultWithDiagnostics.Failure(*diagnostics.toTypedArray(), e.asDiagnostics())
        }
    }
}

