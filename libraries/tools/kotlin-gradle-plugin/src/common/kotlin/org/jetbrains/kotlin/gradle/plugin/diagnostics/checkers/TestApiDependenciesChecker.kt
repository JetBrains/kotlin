/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.gradle.plugin.diagnostics.checkers

import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.await
import org.jetbrains.kotlin.gradle.plugin.diagnostics.*
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics.CompilationDependenciesPair
import org.jetbrains.kotlin.gradle.plugin.diagnostics.checkers.IncorrectCompileOnlyDependenciesChecker.stringCoordinates

/**
 * Warn if a 'test' compilation has 'testApi' dependencies.
 *
 * 'testApi' dependencies don't make sense, because projects don't depend on other test dependencies.
 *
 * 'testApi' dependencies will be removed as part of project-level dependencies work KT-71289.
 */
internal object TestApiDependenciesChecker : KotlinGradleProjectChecker {

    override suspend fun KotlinGradleProjectCheckerContext.runChecks(collector: KotlinToolingDiagnosticsCollector) {
        KotlinPluginLifecycle.Stage.ReadyForExecution.await()

        val multiplatform = multiplatformExtension ?: return

        val testCompilationsWithApiDependencies = multiplatform.targets
            .flatMap { target -> testApiDependencies(target) }

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
            val apiDependencies = project.configurations
                .findByName(compilation.apiConfigurationName)
                ?.allDependencies
                .orEmpty()

            CompilationDependenciesPair(
                compilation,
                apiDependencies.map { it.stringCoordinates() },
            )
        }
    }
}
