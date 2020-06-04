/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.ide_services.test_util

import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.mainKts.impl.IvyResolver
import java.io.File
import kotlin.script.dependencies.ScriptContents
import kotlin.script.experimental.api.*
import kotlin.script.experimental.dependencies.*
import kotlin.script.experimental.jvm.withUpdatedClasspath

// in case of flat or direct resolvers the value should be a direct path or file name of a jar respectively
// in case of maven resolver the maven coordinates string is accepted (resolved with com.jcabi.aether library)
@Target(AnnotationTarget.FILE)
@Repeatable
@Retention(AnnotationRetention.SOURCE)
annotation class DependsOn(val value: String = "")

open class ScriptDependenciesResolver {

    private val resolver = CompoundDependenciesResolver(FileSystemDependenciesResolver(), IvyResolver())
    private val addedClasspath = mutableListOf<File>()

    fun resolveFromAnnotations(script: ScriptContents): ResultWithDiagnostics<List<File>> {
        val scriptDiagnostics = mutableListOf<ScriptDiagnostic>()
        val classpath = mutableListOf<File>()

        script.annotations.forEach { annotation ->
            when (annotation) {
                is DependsOn -> {
                    try {
                        when (val result = runBlocking { resolver.resolve(annotation.value) }) {
                            is ResultWithDiagnostics.Failure -> {
                                val diagnostics = ScriptDiagnostic(
                                    ScriptDiagnostic.unspecifiedError,
                                    "Failed to resolve ${annotation.value}:\n" + result.reports.joinToString("\n") { it.message })
                                scriptDiagnostics.add(diagnostics)
                            }
                            is ResultWithDiagnostics.Success -> {
                                addedClasspath.addAll(result.value)
                                classpath.addAll(result.value)
                            }
                        }
                    } catch (e: Exception) {
                        val diagnostic =
                            ScriptDiagnostic(ScriptDiagnostic.unspecifiedError, "Unhandled exception during resolve", exception = e)
                        scriptDiagnostics.add(diagnostic)
                    }
                }
                else -> throw Exception("Unknown annotation ${annotation.javaClass}")
            }
        }
        return if (scriptDiagnostics.isEmpty()) classpath.asSuccess()
        else makeFailureResult(scriptDiagnostics)
    }
}

fun configureMavenDepsOnAnnotations(
    context: ScriptConfigurationRefinementContext,
    resolver: ScriptDependenciesResolver
): ResultWithDiagnostics<ScriptCompilationConfiguration> {
    val annotations = context.collectedData?.get(ScriptCollectedData.foundAnnotations)?.takeIf { it.isNotEmpty() }
        ?: return context.compilationConfiguration.asSuccess()
    val scriptContents = object : ScriptContents {
        override val annotations: Iterable<Annotation> = annotations
        override val file: File? = null
        override val text: CharSequence? = null
    }
    return try {
        resolver.resolveFromAnnotations(scriptContents)
            .onSuccess { classpath ->
                context.compilationConfiguration
                    .let { if (classpath.isEmpty()) it else it.withUpdatedClasspath(classpath) }
                    .asSuccess()
            }
    } catch (e: Throwable) {
        ResultWithDiagnostics.Failure(e.asDiagnostics(path = context.script.locationId))
    }
}
