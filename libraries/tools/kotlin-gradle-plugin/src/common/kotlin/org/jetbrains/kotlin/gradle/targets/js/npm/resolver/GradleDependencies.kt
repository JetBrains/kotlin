/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm.resolver

import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.initialization.IncludedBuild
import java.io.File
import java.io.Serializable

/**
 * _This is an internal KGP utility and should not be used in user buildscripts._
 *
 * Represents a dependency on a Kotlin/JS project from a remote repository.
 *
 * The npm dependencies are specified in the `package.json` file that is packed into the `.klib`.
 * The `package.json` is extracted in
 * [org.jetbrains.kotlin.gradle.targets.js.npm.resolver.KotlinCompilationNpmResolution.createPreparedResolution].
 *
 * Gradle might not have downloaded the file yet.
 * To avoid eagerly downloading, the file is downloaded later and this
 * dependency will be transformed to [FileExternalGradleDependency].
 *
 * KBT should look at removing this and replacing it with an artifact transformer,
 * but there is no planned work yet.
 *
 * @see KotlinCompilationNpmResolver
 * @see FileExternalGradleDependency
 * @see org.jetbrains.kotlin.gradle.targets.js.npm.resolver.KotlinCompilationNpmResolver.ConfigurationVisitor.visitArtifact
 * @see org.gradle.api.artifacts.ResolvedDependency
 */
data class ExternalGradleDependency(
    val dependency: ResolvedDependency,
    val artifact: ResolvedArtifact,
) : Serializable


/**
 * _This is an internal KGP utility and should not be used in user buildscripts._
 *
 * Represents a dependency declared in a buildscript on some existing files on disk.
 *
 * E.g. `implementation(files("some-dir"))`
 *
 * We expect that the files are `.klib` files that contain `package.json` files.
 *
 * The `package.json` files are extracted in
 * [org.jetbrains.kotlin.gradle.targets.js.npm.resolver.KotlinCompilationNpmResolution.createPreparedResolution].
 *
 * Note that Gradle are deprecating
 * [org.gradle.api.artifacts.SelfResolvingDependency]
 * (which [org.gradle.api.artifacts.FileCollectionDependency] implements),
 * so KBT might need to reimplement this.
 *
 * @see KotlinCompilationNpmResolver
 * @see org.gradle.api.artifacts.FileCollectionDependency
 */
data class FileCollectionExternalGradleDependency(
    val files: Collection<File>,
    val dependencyVersion: String?,
) : Serializable

/**
 * _This is an internal KGP utility and should not be used in user buildscripts._
 *
 * Represents a **downloaded** dependency on a Kotlin/JS project from a remote repository.
 *
 * @see KotlinCompilationNpmResolver
 * @see ExternalGradleDependency
 */
data class FileExternalGradleDependency(
    val dependencyName: String,
    val dependencyVersion: String,
    val file: File,
) : Serializable

/**
 * _This is an internal KGP utility and should not be used in user buildscripts._
 *
 * Represents a dependency on a Kotlin/JS project from a Composite build.
 *
 * Used to manually declare task dependencies for
 * [org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinPackageJsonTask].
 *
 * @see KotlinCompilationNpmResolver
 */
data class CompositeDependency(
    @Deprecated("No longer used. Scheduled for removal in Kotlin 2.4.", level = DeprecationLevel.ERROR)
    val dependencyName: String,
    @Deprecated("No longer used. Scheduled for removal in Kotlin 2.4.", level = DeprecationLevel.ERROR)
    val dependencyVersion: String,
    @Deprecated("No longer used. Scheduled for removal in Kotlin 2.4.", level = DeprecationLevel.ERROR)
    val includedBuildDir: File,
    @Transient
    val includedBuild: IncludedBuild?,
) : Serializable

/**
 * _This is an internal KGP utility and should not be used in user buildscripts._
 *
 * Represents a dependency on another Kotlin/JS project from within the same Gradle build.
 *
 * E.g. `implementation(project(":some-other-project))`
 *
 * @see KotlinCompilationNpmResolver
 * @see org.jetbrains.kotlin.gradle.targets.js.npm.resolver.KotlinCompilationNpmResolution.createPreparedResolution
 */
data class InternalDependency(
    val projectPath: String,
    val compilationName: String,
    val projectName: String,
) : Serializable
