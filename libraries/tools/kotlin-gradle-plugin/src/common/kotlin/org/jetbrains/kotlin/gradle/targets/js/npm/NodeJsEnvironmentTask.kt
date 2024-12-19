/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import org.gradle.api.Task
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.work.NormalizeLineEndings
import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.targets.js.nodejs.PackageManagerEnvironment

internal interface NodeJsEnvironmentTask : Task, UsesKotlinNpmResolutionManager {
    @get:Internal
    @InternalKotlinGradlePluginApi
    val nodeJsEnvironment: Property<NodeJsEnvironment>

    @get:Internal
    @InternalKotlinGradlePluginApi
    val packageManagerEnv: Property<PackageManagerEnvironment>

    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:IgnoreEmptyDirectories
    @get:NormalizeLineEndings
    @get:InputFiles
    val packageJsonFiles: ListProperty<RegularFile>
}