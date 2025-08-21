/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import org.gradle.api.file.RegularFile
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.IgnoreEmptyDirectories
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.work.NormalizeLineEndings

/**
 * Represents a task that operates on a collection of `package.json` files.
 *
 * This task requires access to the Node.js environment and the Kotlin NPM resolution manager,
 * inheriting relevant behaviors from the `NodeJsEnvironmentTask` and `UsesKotlinNpmResolutionManager` interfaces.
 *
 * The primary input of this task is a list of `package.json` files, allowing tasks to process
 * or interact with these files in a Node.js project setup.
 */
internal interface PackageJsonFilesTask : NodeJsEnvironmentTask, UsesKotlinNpmResolutionManager {
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:IgnoreEmptyDirectories
    @get:NormalizeLineEndings
    @get:InputFiles
    val packageJsonFiles: ListProperty<RegularFile>
}