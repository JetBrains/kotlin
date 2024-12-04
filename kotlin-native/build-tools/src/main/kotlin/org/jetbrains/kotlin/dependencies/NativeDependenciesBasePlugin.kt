/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.dependencies

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.konan.target.registerTargetWithSanitizerAttribute

/**
 * Base plugin for projects that use native dependencies.
 *
 * For defining native dependencies see [NativeDependenciesDownloaderPlugin].
 * For consuming native dependencies see [NativeDependenciesPlugin]
 *
 * @see NativeDependenciesDownloaderPlugin
 * @see NativeDependenciesPlugin
 */
// TODO: Consider making it more like a standard gradle plugins that also
//       creates default configurations and lifecycle tasks.
class NativeDependenciesBasePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.dependencies.attributesSchema {
            registerTargetWithSanitizerAttribute()
        }
    }
}