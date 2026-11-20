/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.npm

import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.targets.js.npm.NPM_DEP_FILE_VERSION_PREFIX
import org.jetbrains.kotlin.gradle.utils.normalizedAbsoluteFile

/**
 * The version spec, encoded for use in a `package.json` file.
 *
 * The value could be a version, a range, a path, or a special value like `file:./path/to/file`.
 */
internal fun KotlinNpmDependency.encodedVersionSpec(): Provider<String> {
    return when (this) {
        is KotlinNpmDependency.LocalPath -> directory.map { it.asFile }.map {
            "$NPM_DEP_FILE_VERSION_PREFIX${it.normalizedAbsoluteFile()}"
        }
        is KotlinNpmDependency.Package -> versionSpec
    }
}

@JvmName("\$KotlinNpmDependencyPackage")
// use $ to indicate this is synthetic, only required for JVM compatibility but not intended to be used by Java users.
internal fun KotlinNpmDependency(
    name: String,
    dependencyType: KotlinNpmDependency.DependencyType,
    version: Provider<String>,
): KotlinNpmDependency =
    KotlinNpmDependency.Package(
        name,
        dependencyType,
        version,
    )

@JvmName("\$KotlinNpmDependencyLocalPath")
// use $ to indicate this is synthetic, only required for JVM compatibility but not intended to be used by Java users.
internal fun KotlinNpmDependency(
    name: String,
    dependencyType: KotlinNpmDependency.DependencyType,
    directory: Provider<Directory>,
): KotlinNpmDependency =
    KotlinNpmDependency.LocalPath(
        name,
        dependencyType,
        directory,
    )
