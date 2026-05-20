/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import org.gradle.api.Task
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.work.NormalizeLineEndings
import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi

/**
 * A Gradle [org.gradle.api.Task] that uses npm dependencies.
 *
 * @see RequiresNpmDependencies
 */
interface RequiresNpmDependenciesTask : RequiresNpmDependencies, Task {

    /**
     * Temporary replacement, because [RequiresNpmDependencies.getPath] is deprecated.
     *
     * Scheduled for removal in Kotlin 2.7.
     *
     * This function cannot be deprecated because [Task.getPath] is not deprecated.
     * https://youtrack.jetbrains.com/issue/KTLC-156/Do-not-propagate-method-deprecation-through-overrides.
     *
     * @see Task.getPath
     */
    @Internal
    override fun getPath(): String

    /**
     * _This is an internal KGP utility and should not be used in user buildscripts._
     *
     * The lock files for all npm dependencies.
     *
     * - Lock files for user dependencies.
     * - KGP's npm tooling lock files (for WasmJS).
     *
     * This is only required for accurate up-to-date checks.
     * The value is not intended to be read during task execution.
     */
    @InternalKotlinGradlePluginApi
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    @get:NormalizeLineEndings
    val npmDependenciesLockFiles: ConfigurableFileCollection

    /**
     * _This is an internal KGP utility and should not be used in user buildscripts._
     *
     * The locations of all file-based npm dependencies.
     *
     * If this list is non-empty, caching must be disabled for the task.
     *
     * (The locations are stored for better logging messages.)
     */
    @InternalKotlinGradlePluginApi
    @get:Internal
    val fileBasedNpmDependencyLocations: ListProperty<String>
}
