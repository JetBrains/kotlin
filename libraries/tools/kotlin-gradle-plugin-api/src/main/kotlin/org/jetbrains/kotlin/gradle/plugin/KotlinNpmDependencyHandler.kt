/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.artifacts.Dependency
import java.io.File

interface KotlinNpmDependencyHandler {
    fun npm(name: String): Dependency

    fun npm(name: String, version: String): Dependency

    fun npm(name: String, directory: File): Dependency

    fun npm(directory: File): Dependency

    @Deprecated(
        message = "Use npm(name, version) instead. Name like in package.json"
    )
    fun npm(org: String? = null, packageName: String, version: String = "*"): Dependency

    fun devNpm(name: String, version: String): Dependency

    fun devNpm(name: String, directory: File): Dependency

    fun devNpm(directory: File): Dependency

    fun optionalNpm(name: String, version: String): Dependency

    fun optionalNpm(name: String, directory: File): Dependency

    fun optionalNpm(directory: File): Dependency

    fun peerNpm(name: String, version: String): Dependency
}