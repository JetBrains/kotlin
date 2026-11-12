/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("TYPEALIAS_EXPANSION_DEPRECATION")

package org.jetbrains.kotlin.gradle.targets.web.npm.shared

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.provider.ProviderFactory
import org.jetbrains.kotlin.gradle.npm.KotlinNpmDependency
import org.jetbrains.kotlin.gradle.npm.KotlinNpmDependency.DependencyType
import org.jetbrains.kotlin.gradle.npm.npmDependenciesCollector
import org.jetbrains.kotlin.gradle.plugin.mpp.disambiguateName
import org.jetbrains.kotlin.gradle.plugin.mpp.isMain
import org.jetbrains.kotlin.gradle.targets.KotlinTargetSideEffect
import org.jetbrains.kotlin.gradle.targets.js.KotlinWasmTargetType
import org.jetbrains.kotlin.gradle.targets.js.NpmPackageVersion
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrCompilation
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmDependencyDeprecated
import org.jetbrains.kotlin.gradle.targets.js.npm.RequiresNpmDependenciesTask
import org.jetbrains.kotlin.gradle.targets.js.npm.toKotlinNpmDependency
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.targets.web.KotlinWebPlatformType
import org.jetbrains.kotlin.gradle.tasks.registerTask
import org.jetbrains.kotlin.gradle.utils.withType

internal val SetupNpmDependencyReportSideEffect = KotlinTargetSideEffect { target ->
    if (target !is KotlinJsIrTarget) return@KotlinTargetSideEffect
    val platform = when (target.wasmTargetType) {
        null -> KotlinWebPlatformType.JS
        KotlinWasmTargetType.JS -> KotlinWebPlatformType.WasmJS
        KotlinWasmTargetType.WASI -> return@KotlinTargetSideEffect
    }

    val project = target.project
    val platformDirectoryName = platform.platformName

    project.registerOncePackageJsonToNpmDependencyReportTransform(target, platform)
    val npmDependenciesReport = project.maybeCreateNpmDependenciesReport(platform)

    target.compilations.all { compilation ->
        val npmRuntimeDependenciesResolver = project.createNpmRuntimeDependenciesResolver(compilation)
        val reportTask = project.registerTask<KotlinNpmDependencyReportTask>(compilation.npmDependenciesReportTaskName) { task ->
            task.description = "Collects the npm dependencies of $compilation."
            task.target.convention(platformDirectoryName)
            task.compilation.configure(project, compilation, npmRuntimeDependenciesResolver)
            task.outputFile.convention(
                project.layout.buildDirectory.file(
                    compilation.outputModuleName.map { "kotlin-npm/$platformDirectoryName/npm-dep-reports/$it/$NPM_DEPENDENCY_REPORT_FILE_NAME" }
                )
            )
        }

        val reportFile = reportTask.flatMap { it.outputFile }
        npmDependenciesReport.outgoing.artifact(reportFile)
        if (compilation.isMain()) {
            project.createNpmRuntimeDependenciesVariant(target, reportFile)
        }
    }
}

private val KotlinJsIrCompilation.npmDependenciesReportTaskName: String
    get() = disambiguateName("npmDependenciesReport")

private fun CompilationNpmDependencies.configure(
    project: Project,
    compilation: KotlinJsIrCompilation,
    npmRuntimeDependenciesResolver: Configuration,
) {
    moduleName.convention(compilation.outputModuleName)
    directUserDependencies.convention(project.provider {
        collectDirectDeclaredNpmDependencies(
            compilation,
            npmRuntimeDependenciesResolver,
            project.providers,
        )
    })
    npmToolingDependencies.convention(project.provider { project.collectDevNpmDependencies(compilation) })
    dependencyReports.from(project.npmRuntimeDependenciesReports(npmRuntimeDependenciesResolver))
}

private fun collectDirectDeclaredNpmDependencies(
    compilation: KotlinJsIrCompilation,
    npmRuntimeDependenciesResolver: Configuration,
    providers: ProviderFactory,
): Set<KotlinNpmDependency> {
    val fromCollectors = (compilation.allAssociatedCompilations + compilation)
        .flatMap { it.allKotlinSourceSets }
        .flatMap { sourceSet -> sourceSet.npmDependenciesCollector.npmDependencies.get() }
    val fromConfigurations = npmRuntimeDependenciesResolver.allDependencies
        .withType(NpmDependencyDeprecated::class.java)
        .map { it.toKotlinNpmDependency(providers) }
    return (fromCollectors + fromConfigurations).toSet()
}

private fun Project.collectDevNpmDependencies(compilation: KotlinJsIrCompilation): Set<KotlinNpmDependency> =
    tasks.withType<RequiresNpmDependenciesTask>()
        .filter { task ->
            task.enabled &&
                    task.compilation.disambiguatedName == compilation.disambiguatedName &&
                    !(task is KotlinJsTest && task.testFramework == null)
        }
        .flatMap { it.requiredNpmDependencies }
        .filterIsInstance<NpmPackageVersion>()
        .mapTo(mutableSetOf()) {
            KotlinNpmDependency(
                it.name,
                DependencyType.DEV,
                provider { it.version },
            )
        }
