/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import org.gradle.api.Task
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity.NAME_ONLY
import org.gradle.work.NormalizeLineEndings
import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.targets.js.RequiredKotlinJsDependency
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrCompilation

/**
 * Represents a KGP util that requires npm dependencies.
 *
 * **Note:** This interface is not intended for implementation by build script or plugin authors.
 */
interface RequiresNpmDependencies {
    val compilation: KotlinJsIrCompilation

    val requiredNpmDependencies: Set<RequiredKotlinJsDependency>

//    @Deprecated("No longer required. Scheduled for removal in Kotlin 2.7.")
    /**
     * Do not use.
     * No longer required. Scheduled for removal in Kotlin 2.7.
     *
     * Moved to [RequiresNpmDependenciesTask].
     */
    @Internal
    fun getPath(): String
}

/**
 * A Gradle [Task] that uses npm dependencies.
 */
//@InternalKotlinGradlePluginApi
internal interface RequiresNpmDependenciesTask : RequiresNpmDependencies, Task {

    /**
     * The lockfiles for all npm dependencies.
     *
     * This is only required for accurate up-to-date checks.
     */
    @InternalKotlinGradlePluginApi
    @get:InputFiles
    @get:PathSensitive(NAME_ONLY)
    @get:NormalizeLineEndings
    val npmDependenciesLockFiles: ConfigurableFileCollection
}
