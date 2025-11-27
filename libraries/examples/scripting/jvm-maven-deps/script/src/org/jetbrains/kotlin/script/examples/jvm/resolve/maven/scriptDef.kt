/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.script.examples.jvm.resolve.maven

import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.api.*
import kotlin.script.experimental.dependencies.*
import kotlin.script.experimental.dependencies.maven.MavenDependenciesResolver
import kotlin.script.experimental.jvm.JvmDependency
import kotlin.script.experimental.jvm.dependenciesFromCurrentContext
import kotlin.script.experimental.jvm.jvm

@KotlinScript(
    fileExtension = "scriptwithdeps.kts",
    compilationConfiguration = ScriptWithMavenDepsConfiguration::class
)
abstract class ScriptWithMavenDeps

object ScriptWithMavenDepsConfiguration : ScriptCompilationConfiguration(
    {
        defaultImports(DependsOn::class, Repository::class)
        jvm {
            dependenciesFromCurrentContext(
                "scripting-jvm-maven-deps", // script library jar name
                "kotlin-scripting-dependencies" // DependsOn annotation is taken from it
            )
        }
        refineConfiguration {
//            @Suppress("DEPRECATION")
//            onAnnotations(DependsOn::class, Repository::class, handler = ::configureMavenDepsOnAnnotations)
            onFir(handler = ::configureMavenDepsOnFir)
        }
    }
) {
    @Suppress("unused")
    private fun readResolve(): Any = ScriptWithMavenDepsConfiguration
}

private val resolver = CompoundDependenciesResolver(FileSystemDependenciesResolver(), MavenDependenciesResolver())

fun configureMavenDepsOnAnnotations(context: ScriptConfigurationRefinementContext): ResultWithDiagnostics<ScriptCompilationConfiguration> {
    val annotations = context.collectedData?.get(ScriptCollectedData.collectedAnnotations)?.takeIf { it.isNotEmpty() }
        ?: return context.compilationConfiguration.asSuccess()
    return runBlocking {
        resolver.resolveFromScriptSourceAnnotations(annotations)
    }.onSuccess {
        context.compilationConfiguration.with {
            dependencies.append(JvmDependency(it))
        }.asSuccess()
    }
}

private fun configureMavenDepsOnFir(context: ScriptConfigurationRefinementContext): ResultWithDiagnostics<ScriptCompilationConfiguration> {
    val fir = context.collectedData?.get(ScriptCollectedData.fir) ?: return context.compilationConfiguration.asSuccess()
    val annotations = fir.flatMap {
        it.annotations.mapNotNull {
            (it as? FirAnnotationCall)?.toAnnotationObjectIfMatches(DependsOn::class, Repository::class)?.let {
                ScriptSourceAnnotation(it, null)
            }
        }
    }
    if (annotations.isEmpty()) return context.compilationConfiguration.asSuccess()
    return runBlocking {
        resolver.resolveFromScriptSourceAnnotations(annotations)
    }.onSuccess {
        context.compilationConfiguration.with {
            dependencies.append(JvmDependency(it))
        }.asSuccess()
    }
}

