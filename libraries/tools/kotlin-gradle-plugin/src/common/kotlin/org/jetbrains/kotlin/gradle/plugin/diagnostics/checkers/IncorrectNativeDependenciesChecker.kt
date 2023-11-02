/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics.checkers

import org.gradle.api.artifacts.Dependency
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.await
import org.jetbrains.kotlin.gradle.plugin.diagnostics.*
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinGradleProjectChecker
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinGradleProjectCheckerContext
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnosticsCollector
import org.jetbrains.kotlin.gradle.plugin.diagnostics.reportDiagnostic
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

internal object IncorrectNativeDependenciesChecker : KotlinGradleProjectChecker {
    override suspend fun KotlinGradleProjectCheckerContext.runChecks(collector: KotlinToolingDiagnosticsCollector) {
        KotlinPluginLifecycle.Stage.AfterFinaliseDsl.await()
        if (PropertiesProvider(project).ignoreIncorrectNativeDependencies == true) return

        KotlinPluginLifecycle.Stage.ReadyForExecution.await()
        val multiplatform = multiplatformExtension ?: return
        multiplatform.targets.filterIsInstance<KotlinNativeTarget>().forEach { target ->
            checkIncorrectDependencies(target)
        }
    }

    private fun KotlinGradleProjectCheckerContext.checkIncorrectDependencies(target: KotlinNativeTarget) {
        val compileOnlyDependencies = target.compilations.mapNotNull {
            val dependencies = project.configurations.getByName(it.compileOnlyConfigurationName).allDependencies
            if (dependencies.isNotEmpty()) {
                it to dependencies
            } else null
        }

        fun Dependency.stringCoordinates(): String = buildString {
            group?.let { append(it).append(':') }
            append(name)
            version?.let { append(':').append(it) }
        }

        compileOnlyDependencies.forEach { (compilation, dependencies) ->
            project.reportDiagnostic(
                KotlinToolingDiagnostics.IncorrectNativeDependenciesWarning(
                    targetName = target.name,
                    compilationName = compilation.name,
                    dependencies = dependencies.map { it.stringCoordinates() }
                )
            )
        }
    }
}
