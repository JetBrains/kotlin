/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.gradle.plugin.diagnostics.checkers

import org.jetbrains.kotlin.gradle.dsl.kotlinJvmExtensionOrNull
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.await
import org.jetbrains.kotlin.gradle.plugin.diagnostics.*
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics.CompilationDependenciesPair
import org.jetbrains.kotlin.gradle.utils.stringCoordinates

/**
 * Warn if a 'test' compilation has 'testApi' dependencies.
 *
 * 'testApi' dependencies don't make sense, because projects don't depend on other project's test dependencies.
 *
 * ### Limitations
 *
 * This diagnostic is only best effort.
 * 'testApi' dependencies will be removed as part of project-level dependencies work KT-71289.
 *
 * Due to limitations in the diagnostic checks, the diagnostic is simple.
 * This means it will not detect 'testApi' dependencies in some situations.
 *
 * - Because diagnostics run in the configuration phase, not execution phase,
 *   it only checks if the `testApi` configuration has declared dependencies.
 * - Ignore dependencies that are wrapped in `Provider<>`s.
 *   This is to prevent eagerly evaluating lazily declared dependencies.
 *
 * We expect such uses to be limited.
 */
internal object TestApiDependenciesChecker : KotlinGradleProjectChecker {

    override suspend fun KotlinGradleProjectCheckerContext.runChecks(collector: KotlinToolingDiagnosticsCollector) {
        KotlinPluginLifecycle.Stage.ReadyForExecution.await()

        val kotlinTargets =
            sequence {
                project.kotlinJvmExtensionOrNull?.let { kotlinJvm ->
                    yield(kotlinJvm.target)
                }
                project.multiplatformExtensionOrNull?.let { kotlinMultiplatform ->
                    yieldAll(kotlinMultiplatform.targets)
                }
            }

        val testCompilationsWithApiDependencies = kotlinTargets
            .flatMap { target -> testApiDependencies(target) }
            .toList()

        if (testCompilationsWithApiDependencies.any { it.dependencyCoords.isNotEmpty() }) {
            project.reportDiagnostic(
                KotlinToolingDiagnostics.TestApiDependencyWarning(
                    testCompilationsWithApiDependencies = testCompilationsWithApiDependencies,
                )
            )
        }
    }

    /**
     * Get all declared `testApi` dependencies, grouped by compilation.
     *
     * Fetches Configurations leniently, just in case a plugin (e.g. AGP) isn't configured correctly.
     */
    private fun KotlinGradleProjectCheckerContext.testApiDependencies(
        target: KotlinTarget,
    ): List<CompilationDependenciesPair> {

        val testCompilations = target.compilations
            .filter { it.name == KotlinCompilation.TEST_COMPILATION_NAME }

        return testCompilations.map { compilation ->
            val apiDependencies = mutableListOf<String>()
            compilation.allKotlinSourceSets.forEach { sourceSet ->
                project.configurations
                    .findByName(sourceSet.apiConfigurationName)
                    ?.dependencies
                    ?.configureEach { dependency ->
                        apiDependencies += dependency.stringCoordinates()
                    }
            }
            CompilationDependenciesPair(
                compilation,
                apiDependencies,
            )
        }
    }
}
