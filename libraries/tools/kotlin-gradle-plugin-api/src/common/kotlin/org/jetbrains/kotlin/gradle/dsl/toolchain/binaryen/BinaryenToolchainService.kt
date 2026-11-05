/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dsl.toolchain.binaryen

import org.jetbrains.kotlin.gradle.ExperimentalKotlinToolchainDsl
import org.jetbrains.kotlin.gradle.dsl.toolchain.DownloadedToolchainParameters
import org.jetbrains.kotlin.gradle.dsl.toolchain.ToolchainManagementDsl
import org.jetbrains.kotlin.gradle.dsl.toolchain.KotlinToolchainService
import org.jetbrains.kotlin.gradle.dsl.toolchain.PreInstalledToolchainParameters

/**
 * Provisions Binaryen, the WebAssembly optimizer (`wasm-opt`) used to optimize Kotlin/Wasm binaries.
 *
 * @see KotlinToolchainService
 */
@ExperimentalKotlinToolchainDsl
interface BinaryenToolchainService<P : KotlinToolchainService.Parameters> : KotlinToolchainService<P>

/**
 * The default [BinaryenToolchainService] used by the Kotlin Gradle Plugin.
 *
 * Downloads the requested Binaryen distribution and installs it into a shared, machine-wide directory,
 * so that a distribution is downloaded once and then reused by all builds on the machine.
 */
@ExperimentalKotlinToolchainDsl
interface DefaultBinaryenToolchainService : BinaryenToolchainService<DownloadedToolchainParameters>

/**
 * A [BinaryenToolchainService] that uses a pre-installed `wasm-opt` instead of downloading one.
 *
 * The Binaryen installation is not provisioned; it must be available on the machine that runs the build.
 */
@ExperimentalKotlinToolchainDsl
interface PreInstalledBinaryenToolchainService : BinaryenToolchainService<PreInstalledToolchainParameters>

/**
 * A [BinaryenToolchainService] that disables Binaryen toolchain provisioning.
 *
 * Without a Binaryen toolchain, Binaryen is configured and provisioned per project, using the Binaryen
 * environment spec and the Binaryen setup task, instead of the build-wide toolchain service.
 *
 * Selected with [BinaryenToolchainManagementDsl.disable].
 */
@ExperimentalKotlinToolchainDsl
interface DisabledBinaryenToolchainService : BinaryenToolchainService<KotlinToolchainService.Parameters>

/**
 * `binaryen { ... }` block of the `toolchainManagement { ... }` DSL.
 *
 * ```
 * // settings.gradle.kts
 * kotlin {
 *     toolchainManagement {
 *         binaryen {
 *             toolchainService(PreInstalledBinaryenToolchainService::class) {
 *                 executable.set("/usr/local/bin/wasm-opt")
 *             }
 *         }
 *     }
 * }
 * ```
 *
 * **Note:** This interface is not intended for implementation by build script or plugin authors.
 *
 * @since 2.5.20
 */
@ExperimentalKotlinToolchainDsl
interface BinaryenToolchainManagementDsl : ToolchainManagementDsl
