/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.web.npm.shared

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.npm.DefaultKotlinNpmDependency
import org.jetbrains.kotlin.gradle.npm.KotlinNpmDependency
import org.jetbrains.kotlin.gradle.npm.npmDependenciesCollector
import org.jetbrains.kotlin.gradle.plugin.mpp.isMain
import org.jetbrains.kotlin.gradle.targets.KotlinTargetSideEffect
import org.jetbrains.kotlin.gradle.targets.js.KotlinWasmTargetType
import org.jetbrains.kotlin.gradle.targets.js.NpmPackageVersion
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrCompilation
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import org.jetbrains.kotlin.gradle.targets.js.nodejs.JsPlatformDisambiguator
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmProject
import org.jetbrains.kotlin.gradle.targets.js.npm.RequiresNpmDependenciesTask
import org.jetbrains.kotlin.gradle.targets.js.npm.npmProject
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.targets.js.webTargetVariant
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmPlatformDisambiguator
import org.jetbrains.kotlin.gradle.targets.web.HasPlatformDisambiguator
import org.jetbrains.kotlin.gradle.targets.web.npm.shared.KotlinSharedPackageJsonTask.Companion.sharedPackageJsonTaskName
import org.jetbrains.kotlin.gradle.tasks.locateOrRegisterTask
import org.jetbrains.kotlin.gradle.utils.withType

internal val SetupSharedPackageJsonSideEffect = KotlinTargetSideEffect { target ->
    if (target !is KotlinJsIrTarget) return@KotlinTargetSideEffect
    if (target.wasmTargetType == KotlinWasmTargetType.WASI) return@KotlinTargetSideEffect

    val project = target.project
    val platform = target.webTargetVariant(
        jsVariant = JsPlatformDisambiguator,
        wasmVariant = WasmPlatformDisambiguator,
    )
    val platformDirectoryName = platform.platformDisambiguator ?: JsPlatformDisambiguator.jsPlatform

    val sharedPackageJsonTask = project.locateOrRegisterTask<KotlinSharedPackageJsonTask>(platform.sharedPackageJsonTaskName) { task ->
        task.description = "Collects the npm dependencies of the '$platformDirectoryName' compilations into one package.json per compilation."
        task.outputDirectory.set(project.layout.buildDirectory.dir("kotlin-npm/$platformDirectoryName/shared-package-json"))
    }

    val packageJsonForDependents = project.maybeCreatePackageJsonForDependents(target, platform)
    val packageJsonsForRoot = project.maybeCreatePackageJsonsForRoot(platform)

    target.compilations.all { compilation ->
        sharedPackageJsonTask.configure { task ->
            task.compilations.add(project.compilationPackageJsonInputs(compilation))
        }

        val packageJsonFile = sharedPackageJsonTask.flatMap { task ->
            task.outputDirectory.file(compilation.outputModuleName.map { "$it/${NpmProject.PACKAGE_JSON}" })
        }
        project.artifacts.add(packageJsonsForRoot.name, packageJsonFile)
        if (compilation.isMain()) {
            project.artifacts.add(packageJsonForDependents.name, packageJsonFile)
        }
    }
}

private fun Project.compilationPackageJsonInputs(
    compilation: KotlinJsIrCompilation,
): CompilationPackageJsonInputs = objects.newInstance(CompilationPackageJsonInputs::class.java).apply {
    val npmProject = compilation.npmProject
    moduleName.set(compilation.outputModuleName)
    packageVersion.set(version.toString())
    main.set(npmProject.main)
    types.set(npmProject.typesFilePath)
    directDependencies.set(provider { collectDirectDeclaredNpmDependencies(compilation) })
    toolDependencies.set(provider { collectDevNpmDependencies(compilation) })
}

private fun collectDirectDeclaredNpmDependencies(compilation: KotlinJsIrCompilation): Set<DefaultKotlinNpmDependency> =
    (compilation.allAssociatedCompilations + compilation)
        .flatMap { it.allKotlinSourceSets }
        .flatMapTo(mutableSetOf()) { sourceSet ->
            sourceSet.npmDependenciesCollector.npmDependencies.get()
        }

private fun Project.collectDevNpmDependencies(compilation: KotlinJsIrCompilation): Set<DefaultKotlinNpmDependency> =
    tasks.withType<RequiresNpmDependenciesTask>()
        .filter { task ->
            task.enabled &&
                    task.compilation.disambiguatedName == compilation.disambiguatedName &&
                    !(task is KotlinJsTest && task.testFramework == null)
        }
        .flatMap { it.requiredNpmDependencies }
        .filterIsInstance<NpmPackageVersion>()
        .mapTo(mutableSetOf()) { DefaultKotlinNpmDependency(it.name, it.version, KotlinNpmDependency.Scope.DEV) }
