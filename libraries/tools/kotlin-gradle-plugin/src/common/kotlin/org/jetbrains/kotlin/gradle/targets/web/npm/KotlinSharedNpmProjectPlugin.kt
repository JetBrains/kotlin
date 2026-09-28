/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.web.npm

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.targets.js.nodejs.JsPlatformDisambiguator
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmPlatformDisambiguator
import org.jetbrains.kotlin.gradle.targets.web.npm.shared.setupSharedNpmProject

/**
 * Creates one shared npm project per platform, JS and WasmJS independently, from the
 * package.json files of the projects declared on the `kotlinNpmSharedDependencies` and
 * `kotlinWasmNpmSharedDependencies` configurations. Must be applied to the root project.
 */
@ExperimentalKotlinGradlePluginApi
class KotlinSharedNpmProjectPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        check(project.path == Project.PATH_SEPARATOR) {
            "${this::class.simpleName} can only be applied to the root project of a build, but was applied to '${project.path}'."
        }

        setupSharedNpmProject(project, JsPlatformDisambiguator, rootDirectoryName = JsPlatformDisambiguator.jsPlatform)
        setupSharedNpmProject(project, WasmPlatformDisambiguator, rootDirectoryName = WasmPlatformDisambiguator.platformDisambiguator)
    }
}
