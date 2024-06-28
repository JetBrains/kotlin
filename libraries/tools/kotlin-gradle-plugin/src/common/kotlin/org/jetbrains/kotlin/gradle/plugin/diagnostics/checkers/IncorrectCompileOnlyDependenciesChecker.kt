/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.gradle.plugin.diagnostics.checkers

import org.gradle.api.artifacts.Dependency
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.diagnostics.*
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics.IncorrectCompileOnlyDependencyWarning.CompilationDependenciesPair
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMetadataCompilation

internal object IncorrectCompileOnlyDependenciesChecker : KotlinGradleProjectChecker {

    override suspend fun KotlinGradleProjectCheckerContext.runChecks(collector: KotlinToolingDiagnosticsCollector) {
        KotlinPluginLifecycle.Stage.ReadyForExecution.await()

        val multiplatform = multiplatformExtension ?: return

        val compilationsWithCompileOnlyDependencies = multiplatform.targets
            .filter { target -> !isAllowedCompileOnlyDependencies(target.platformType) }
            .flatMap { target -> compileOnlyDependencies(target) }

        if (compilationsWithCompileOnlyDependencies.any { it.dependencyCoords.isNotEmpty() }) {
            project.reportDiagnostic(
                KotlinToolingDiagnostics.IncorrectCompileOnlyDependencyWarning(
                    compilationsWithCompileOnlyDependencies = compilationsWithCompileOnlyDependencies,
                )
            )
        }
    }

    /**
     * Extract all dependencies of [target], satisfying:
     * 1. they are `compileOnly`
     * 2. they are not exposed as api elements.
     *
     * Fetches Configurations leniently, just in case a plugin (e.g. AGP) isn't configured correctly.
     */
    private fun KotlinGradleProjectCheckerContext.compileOnlyDependencies(
        target: KotlinTarget,
    ): List<CompilationDependenciesPair> {
        val apiElementsDependencies = project.configurations
            .findByName(target.apiElementsConfigurationName)
            ?.allDependencies
            .orEmpty()

        fun Dependency.isInApiElements(): Boolean =
            apiElementsDependencies.any { it.contentEquals(this) }

        val compilationsIncompatibleWithCompileOnly = target.compilations
            .filter { it.isPublished() }
            .filter { !isAllowedCompileOnlyDependencies(it.target.platformType) }

        return compilationsIncompatibleWithCompileOnly.map { compilation ->
            val compileOnlyDependencies = project.configurations
                .findByName(compilation.compileOnlyConfigurationName)
                ?.allDependencies
                .orEmpty()

            val nonApiCompileOnlyDependencies = compileOnlyDependencies.filter { !it.isInApiElements() }

            CompilationDependenciesPair(
                compilation,
                nonApiCompileOnlyDependencies.map { it.stringCoordinates() },
            )
        }
    }

    /**
     * Estimate whether a [KotlinCompilation] is 'publishable' (i.e. it is a main, non-test compilation).
     */
    private fun KotlinCompilation<*>.isPublished(): Boolean {
        return when (this) {
            is KotlinMetadataCompilation<*> -> true
            else -> name == KotlinCompilation.MAIN_COMPILATION_NAME
        }
    }

    private fun KotlinGradleProjectCheckerContext.isAllowedCompileOnlyDependencies(target: KotlinPlatformType): Boolean {
        return when (target) {
            KotlinPlatformType.jvm,
            KotlinPlatformType.androidJvm,
            -> true

            // Technically, compileOnly dependencies should also be forbidden for
            // common compilations, but in practice such dependencies will
            // filtered down to the actual target-specific compilations.
            // Therefore, to avoid duplicated warning messages for dependencies
            // in commonMain and a ${target}Main, don't check common targets.
            KotlinPlatformType.common,
            -> true

            KotlinPlatformType.wasm,
            KotlinPlatformType.js,
            -> false

            KotlinPlatformType.native -> {
                @Suppress("DEPRECATION")
                PropertiesProvider(project).ignoreIncorrectNativeDependencies == true
            }
        }
    }

    private fun Dependency.stringCoordinates(): String = buildString {
        group?.let { append(it).append(':') }
        append(name)
        version?.let { append(':').append(it) }
    }
}
