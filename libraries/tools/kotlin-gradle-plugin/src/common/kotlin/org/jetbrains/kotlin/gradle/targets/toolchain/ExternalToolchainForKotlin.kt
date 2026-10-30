/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalKotlinToolchainDsl::class)

package org.jetbrains.kotlin.gradle.targets.toolchain

import org.jetbrains.kotlin.gradle.ExperimentalKotlinToolchainDsl
import org.jetbrains.kotlin.gradle.dsl.toolchain.ToolchainPlatform
import org.jetbrains.kotlin.gradle.dsl.toolchain.ToolchainRequest
import org.jetbrains.kotlin.gradle.dsl.toolchain.ToolchainVersion

/**
 * The common supertype of the `Kotlin<Name>Toolchain` classes, for example `KotlinD8Toolchain`.
 *
 * Such a class contains all the information about a requested toolchain distribution - built from a [ToolchainRequest]
 * and the toolchain settings - and knows how the tool names its distributions and lays them out on disk.
 *
 * Instances are used as keys of the installations provisioned by a build, so implementations must be data classes.
 */
internal interface ExternalToolchainForKotlin {

    /**
     * The name of the tool, used in log and error messages, for example `Binaryen`.
     */
    val toolName: String

    val version: ToolchainVersion

    val platform: ToolchainPlatform

    /**
     * The name of the directory the distribution is installed into, inside the installations directory,
     * for example `binaryen-version_133-x86_64-linux`.
     *
     * It must identify the distribution completely, because installations are shared between builds.
     */
    val installationName: String

    /**
     * The path of the tool executable, relative to the installation directory, for example `bin/wasm-opt`.
     */
    val executablePath: String
}

internal fun ToolchainRequest.requiredVersion(toolName: String): ToolchainVersion =
    version.orNull ?: error("A $toolName version must be requested")

internal fun ToolchainRequest.requiredPlatform(toolName: String): ToolchainPlatform =
    platform.orNull ?: error("A $toolName platform must be requested")
