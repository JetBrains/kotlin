/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.nodejs

import org.gradle.api.file.FileCollection
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmApiExecution

/**
 * Represents an extension interface for managing and configuring an NPM-based project in a Kotlin/Gradle context.
 * Provides functionality to interact with the NPM package manager and handle its environment setup.
 *
 * @param Env The type of package manager environment implementation. Must extend [PackageManagerEnvironment].
 * @param NpmApi The type of NPM API execution implementation. Must extend [NpmApiExecution] with the given environment.
 */
interface NpmApiExtension<out Env : PackageManagerEnvironment, out NpmApi : NpmApiExecution<Env>> {
    val name: String

    val packageManager: NpmApi

    val environment: Env

    /**
     * Contains lock files for npm dependencies from the currently enabled package manager (Yarn or npm).
     *
     * The files are not directly read. They are only used for registering Gradle Task outputs
     * (See [org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinNpmInstallTask.additionalFiles]).
     *
     * For JS the lockfile for user-declared and KGP tooling npm dependencies.
     * For WasmJS, only the lockfile for user-declared dependencies.
     */
    val additionalInstallOutput: FileCollection

    val preInstallTasks: ListProperty<TaskProvider<*>>

    val postInstallTasks: ListProperty<TaskProvider<*>>
}

@Deprecated("No longer used. Scheduled for removal in Kotlin 2.3.", ReplaceWith("NpmApiExtension<*, *>"))
@Suppress("unused")
typealias NpmApiExt = NpmApiExtension<PackageManagerEnvironment, NpmApiExecution<PackageManagerEnvironment>>
