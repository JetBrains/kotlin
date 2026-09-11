/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.web.npm

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.targets.web.npm.shared.SharedNpmPlatform
import org.jetbrains.kotlin.gradle.targets.web.npm.shared.setupSharedNpmManagerProject

/**
 * Manages the shared-npm-project: one npm project per platform (JS and WasmJS independently)
 * aggregating the package.json files of all JS/WasmJS subprojects. Must be applied to the
 * root project.
 */
@ExperimentalKotlinGradlePluginApi
class KotlinSharedNpmProjectPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        check(project.path == Project.PATH_SEPARATOR) {
            "${this::class.simpleName} can only be applied to the root project of a build, but was applied to '${project.path}'."
        }

        SharedNpmPlatform.values().forEach { platform ->
            setupSharedNpmManagerProject(project, platform)
        }
    }
}
