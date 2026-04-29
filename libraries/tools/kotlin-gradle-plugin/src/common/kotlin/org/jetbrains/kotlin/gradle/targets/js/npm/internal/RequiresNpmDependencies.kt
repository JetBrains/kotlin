/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm.internal

//import org.gradle.api.Task
//import org.gradle.api.file.RegularFileProperty
//import org.gradle.api.tasks.InputFile
//import org.gradle.api.tasks.PathSensitive
//import org.gradle.api.tasks.PathSensitivity.NAME_ONLY
//import org.gradle.work.NormalizeLineEndings
//import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi
//import org.jetbrains.kotlin.gradle.targets.js.RequiredKotlinJsDependency
//import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrCompilation
//
///**
// * Represents a KGP util that requires npm dependencies.
// *
// * **Note:** This interface is not intended for implementation by build script or plugin authors.
// */
//internal interface RequiresNpmDependencies {
//    @InternalKotlinGradlePluginApi
//    val compilation: KotlinJsIrCompilation
//
//    @InternalKotlinGradlePluginApi
//    val requiredNpmDependencies: Set<RequiredKotlinJsDependency>
//}
//
///**
// * A Gradle [Task] that uses npm dependencies.
// */
//internal interface RequiresNpmDependenciesTask : RequiresNpmDependencies, Task {
//
//    /**
//     * The lockfile for all npm dependencies.
//     *
//     * This is only required for accurate up-to-date checks.
//     */
//    @InternalKotlinGradlePluginApi
//    @get:InputFile
//    @get:PathSensitive(NAME_ONLY)
//    @get:NormalizeLineEndings
//    val npmDependenciesLockfile: RegularFileProperty
//
//    @InternalKotlinGradlePluginApi
//    @get:InputFile
//    @get:PathSensitive(NAME_ONLY)
//    @get:NormalizeLineEndings
//    val npmToolingDependenciesLockfile: RegularFileProperty
//
//}
