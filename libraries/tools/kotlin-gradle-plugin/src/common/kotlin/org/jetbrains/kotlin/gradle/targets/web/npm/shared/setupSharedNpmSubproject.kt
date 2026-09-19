/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.web.npm.shared

import org.jetbrains.kotlin.gradle.plugin.mpp.disambiguateName
import org.jetbrains.kotlin.gradle.targets.KotlinTargetSideEffect
import org.jetbrains.kotlin.gradle.targets.js.KotlinWasmTargetType
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import org.jetbrains.kotlin.gradle.targets.js.nodejs.JsPlatformDisambiguator
import org.jetbrains.kotlin.gradle.targets.js.npm.npmProject
import org.jetbrains.kotlin.gradle.targets.js.webTargetVariant
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmPlatformDisambiguator
import org.jetbrains.kotlin.gradle.targets.web.HasPlatformDisambiguator
import org.jetbrains.kotlin.gradle.targets.web.npm.isolated.KotlinIsolatedPackageJsonTask
import org.jetbrains.kotlin.gradle.targets.web.npm.isolated.isIsolatedNpmResolutionEnabled

internal val PublishSharedPackageJsonSideEffect = KotlinTargetSideEffect { target ->
    if (target !is KotlinJsIrTarget) return@KotlinTargetSideEffect
    if (target.wasmTargetType == KotlinWasmTargetType.WASI) return@KotlinTargetSideEffect

    val project = target.project
    val platform = target.webTargetVariant(
        jsVariant = JsPlatformDisambiguator,
        wasmVariant = WasmPlatformDisambiguator,
    )

    val packageJsonFilesConfiguration = project.maybeCreateConsumableNpmSharedPackageJsonFilesConfiguration(platform)

    val isIsolatedNpmResolutionEnabled = project.isIsolatedNpmResolutionEnabled

    target.compilations.all { compilation ->
        if (isIsolatedNpmResolutionEnabled) {
            // The legacy `package.json` task is registered by `KotlinCompilationNpmResolver`,
            // which is not used when the Isolated Projects compatible NPM resolution is enabled.
            KotlinIsolatedPackageJsonTask.register(compilation, platform)
        }

        val packageJsonTaskName = compilation.disambiguateName("packageJson")
        val packageJsonFile = project.provider { compilation.npmProject.packageJsonFile }.flatMap { it }
        project.artifacts.add(packageJsonFilesConfiguration.name, packageJsonFile) {
            it.builtBy(packageJsonTaskName)
        }
    }
}
