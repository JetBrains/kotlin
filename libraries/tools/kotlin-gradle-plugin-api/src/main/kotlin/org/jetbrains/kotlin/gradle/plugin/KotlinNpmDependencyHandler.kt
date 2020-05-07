/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.artifacts.Dependency
import java.io.File

interface KotlinNpmDependencyHandler {
    @Deprecated("Declaring NPM dependency without version is forbidden")
    fun npm(name: String): Dependency

    fun npm(
        name: String,
        version: String,
        generateKotlinExternals: Boolean
    ): Dependency

    fun npm(
        name: String,
        version: String
    ): Dependency = npm(
        name = name,
        version = version,
        generateKotlinExternals = DEFAULT_GENERATE_KOTLIN_EXTERNALS
    )

    fun npm(
        name: String,
        directory: File,
        generateKotlinExternals: Boolean
    ): Dependency

    fun npm(
        name: String,
        directory: File
    ): Dependency = npm(
        name = name,
        directory = directory,
        generateKotlinExternals = DEFAULT_GENERATE_KOTLIN_EXTERNALS
    )

    fun npm(
        directory: File,
        generateKotlinExternals: Boolean
    ): Dependency

    fun npm(
        directory: File
    ): Dependency = npm(
        directory = directory,
        generateKotlinExternals = DEFAULT_GENERATE_KOTLIN_EXTERNALS
    )

    fun devNpm(
        name: String,
        version: String
    ): Dependency

    fun devNpm(
        name: String,
        directory: File
    ): Dependency

    fun devNpm(
        directory: File
    ): Dependency

    fun optionalNpm(
        name: String,
        version: String,
        generateKotlinExternals: Boolean
    ): Dependency

    fun optionalNpm(
        name: String,
        version: String
    ): Dependency = optionalNpm(
        name = name,
        version = version,
        generateKotlinExternals = DEFAULT_GENERATE_KOTLIN_EXTERNALS
    )

    fun optionalNpm(
        name: String,
        directory: File,
        generateKotlinExternals: Boolean
    ): Dependency

    fun optionalNpm(
        name: String,
        directory: File
    ): Dependency = optionalNpm(
        name = name,
        directory = directory,
        generateKotlinExternals = DEFAULT_GENERATE_KOTLIN_EXTERNALS
    )

    fun optionalNpm(
        directory: File,
        generateKotlinExternals: Boolean
    ): Dependency

    fun optionalNpm(
        directory: File
    ): Dependency = optionalNpm(
        directory = directory,
        generateKotlinExternals = DEFAULT_GENERATE_KOTLIN_EXTERNALS
    )

    fun peerNpm(
        name: String,
        version: String
    ): Dependency
}

const val DEFAULT_GENERATE_KOTLIN_EXTERNALS = true