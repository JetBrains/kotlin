/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.UnresolvedDependencyResult
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.component.resolution.failure.exception.VariantSelectionByAttributesException
import org.gradle.internal.resolve.ModuleVersionResolveException
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinProjectSetupAction
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.ToolingDiagnostic
import org.jetbrains.kotlin.gradle.plugin.diagnostics.UsesKotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.setupKotlinToolingDiagnosticsParameters
import org.jetbrains.kotlin.gradle.plugin.ide.Idea222Api
import org.jetbrains.kotlin.gradle.plugin.ide.ideaImportDependsOn
import org.jetbrains.kotlin.gradle.tasks.dependsOn
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName

/**
 * Reports dependencies that can't be resolved in the context of the given Kotlin Target.
 * I.e., when a dependency doesn't have JAR or klib, that can passed to the compiler classpath.
 *
 * @since 2.2.20
 */
abstract class CheckKotlinMultiplatformDependenciesTask : DefaultTask(), UsesKotlinToolingDiagnostics {
    @get:Internal
    internal abstract val resolutionResult: Property<ResolvedComponentResult>

    @get:Internal
    internal abstract val kotlinTargetName: Property<String>

    /**
     * This is a hack:
     * `lazy {}` creates an instance of [Lazy] that implements [java.io.Serializable] and will be evaluated eagerly upon serialization.
     * When a Gradle task is serialized during configuration cache serialization, all its fields (including [collectAndReportDiagnostics])
     * will be serialized as well.
     * So the body of lazy will be executed and diagnostics reported.
     */
    private val collectAndReportDiagnostics = lazy {
        val diagnostics = collectUnresolvedDependenciesDiagnostics()
        diagnostics.forEach { reportDiagnostic(it) }
        diagnostics
    }

    private fun collectUnresolvedDependenciesDiagnostics(): List<ToolingDiagnostic> {
        val directUnresolvedDependencies = resolutionResult.get().dependencies.filterIsInstance<UnresolvedDependencyResult>()

        return directUnresolvedDependencies.mapNotNull { dependency ->
            val failure = dependency.failure
            if (failure !is ModuleVersionResolveException) return@mapNotNull null
            if (failure.causes.none { it is VariantSelectionByAttributesException }) return@mapNotNull null

            KotlinToolingDiagnostics.DependencyDoesNotSupportKotlinPlatform(
                dependency = dependency.attempted.displayName,
                kotlinTargetName = kotlinTargetName.get()
            )
        }
    }

    @TaskAction
    fun action() {
        // Initialize and force diagnostics to be reported. This is necessary in case if configuration cache serialization isn't enabled
        collectAndReportDiagnostics.value
    }
}

internal val CheckKotlinMultiplatformDependencies = KotlinProjectSetupAction {
    val checkKmpDependenciesForAllTargets = project.tasks.register("checkKotlinMultiplatformDependencies") { task ->
        task.group = "verification"
        task.description = "Check if Kotlin Multiplatform dependencies are resolved for all targets"
    }

    @OptIn(Idea222Api::class)
    project.ideaImportDependsOn(checkKmpDependenciesForAllTargets)

    project.multiplatformExtension.targets.all { target ->
        if (target is KotlinMetadataTarget) return@all

        target.compilations.all { compilation ->
            val classpathConfiguration = project.configurations.getByName(compilation.compileDependencyConfigurationName)

            val checkKmpDependenciesForCompilation = tasks.register(
                lowerCamelCaseName("check", compilation.defaultSourceSet.name, "KmpDependencies"),
                CheckKotlinMultiplatformDependenciesTask::class.java
            ) { task ->
                task.resolutionResult.set(classpathConfiguration.incoming.resolutionResult.rootComponent)
                task.kotlinTargetName.set(target.targetName)

                task.group = "verification"
                task.description = "Check if Kotlin Multiplatform dependencies are resolved for ${target.targetName}"
                task.setupKotlinToolingDiagnosticsParameters(project)
            }

            checkKmpDependenciesForAllTargets.dependsOn(checkKmpDependenciesForCompilation)
            compilation.compileTaskProvider.dependsOn(checkKmpDependenciesForCompilation)
        }
    }
}
