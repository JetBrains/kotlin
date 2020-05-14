/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.script.examples.jvm.resolve.maven

import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.script.util.DependsOn
import org.jetbrains.kotlin.script.util.Repository
import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.api.*
import kotlin.script.experimental.dependencies.CompoundDependenciesResolver
import kotlin.script.experimental.dependencies.FileSystemDependenciesResolver
import kotlin.script.experimental.dependencies.maven.MavenDependenciesResolver
import kotlin.script.experimental.dependencies.tryAddRepository
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
                "kotlin-script-util" // DependsOn annotation is taken from script-util
            )
        }
        refineConfiguration {
            onAnnotations(DependsOn::class, Repository::class, handler = ::configureMavenDepsOnAnnotations)
        }
    }
)

private val resolver = CompoundDependenciesResolver(FileSystemDependenciesResolver(), MavenDependenciesResolver())

fun configureMavenDepsOnAnnotations(context: ScriptConfigurationRefinementContext): ResultWithDiagnostics<ScriptCompilationConfiguration> {
    val annotations = context.collectedData?.get(ScriptCollectedData.foundAnnotations)?.takeIf { it.isNotEmpty() }
        ?: return context.compilationConfiguration.asSuccess()
    annotations.forEach { annotation ->
        when (annotation) {
            is Repository -> {
                val repositoryCoordinates = with(annotation) { value.takeIf { it.isNotBlank() } ?: url }
                if (!resolver.tryAddRepository(repositoryCoordinates))
                    return makeFailureResult("Unrecognized repository coordinates: $repositoryCoordinates")
            }
            is DependsOn -> {}
            else -> return makeFailureResult("Unknown annotation ${annotation.javaClass}")
        }
    }
    return annotations.filterIsInstance(DependsOn::class.java).flatMapSuccess { dep ->
        val artifactCoordinates =
            if (dep.value.isNotBlank()) dep.value
            else listOf(dep.groupId, dep.artifactId, dep.version).filter { it.isNotBlank() }.joinToString(":")
        runBlocking {
            resolver.resolve(artifactCoordinates)
        }
    }.onSuccess {
        context.compilationConfiguration.with {
            dependencies.append(JvmDependency(it))
        }.asSuccess()
    }
}

