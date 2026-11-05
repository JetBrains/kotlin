/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dsl.toolchain.wasmtime

import org.jetbrains.kotlin.gradle.ExperimentalKotlinToolchainDsl
import org.jetbrains.kotlin.gradle.dsl.toolchain.DownloadedToolchainParameters
import org.jetbrains.kotlin.gradle.dsl.toolchain.ToolchainManagementDsl
import org.jetbrains.kotlin.gradle.dsl.toolchain.KotlinToolchainService
import org.jetbrains.kotlin.gradle.dsl.toolchain.PreInstalledToolchainParameters

/**
 * Provisions Wasmtime, the WebAssembly runtime used to run Kotlin/Wasm WASI applications and tests.
 *
 * @see KotlinToolchainService
 */
@ExperimentalKotlinToolchainDsl
interface WasmtimeToolchainService<P : KotlinToolchainService.Parameters> : KotlinToolchainService<P>

/**
 * The default [WasmtimeToolchainService] used by the Kotlin Gradle Plugin.
 *
 * Downloads the requested Wasmtime distribution and installs it into a shared, machine-wide directory,
 * so that a distribution is downloaded once and then reused by all builds on the machine.
 */
@ExperimentalKotlinToolchainDsl
interface DefaultWasmtimeToolchainService : WasmtimeToolchainService<DownloadedToolchainParameters>

/**
 * A [WasmtimeToolchainService] that uses a pre-installed Wasmtime instead of downloading one.
 *
 * The Wasmtime installation is not provisioned; it must be available on the machine that runs the build.
 */
@ExperimentalKotlinToolchainDsl
interface PreInstalledWasmtimeToolchainService : WasmtimeToolchainService<PreInstalledToolchainParameters>

/**
 * A [WasmtimeToolchainService] that disables Wasmtime toolchain provisioning.
 *
 * Without a Wasmtime toolchain, Wasmtime is configured and provisioned per project, using the Wasmtime
 * environment spec and the Wasmtime setup task, instead of the build-wide toolchain service.
 *
 * Selected with [WasmtimeToolchainManagementDsl.disable].
 */
@ExperimentalKotlinToolchainDsl
interface DisabledWasmtimeToolchainService : WasmtimeToolchainService<KotlinToolchainService.Parameters>

/**
 * `wasmtime { ... }` block of the `toolchainManagement { ... }` DSL.
 *
 * ```
 * // settings.gradle.kts
 * kotlin {
 *     toolchainManagement {
 *         wasmtime {
 *             toolchainService {
 *                 installationDir.set(File("/opt/toolchains/wasmtime"))
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
interface WasmtimeToolchainManagementDsl : ToolchainManagementDsl
