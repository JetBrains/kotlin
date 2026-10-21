/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dsl.toolchain.swc

import org.jetbrains.kotlin.gradle.ExperimentalKotlinToolchainDsl
import org.jetbrains.kotlin.gradle.dsl.toolchain.DownloadedToolchainParameters
import org.jetbrains.kotlin.gradle.dsl.toolchain.ToolchainManagementDsl
import org.jetbrains.kotlin.gradle.dsl.toolchain.KotlinToolchainService
import org.jetbrains.kotlin.gradle.dsl.toolchain.PreInstalledToolchainParameters

/**
 * Provisions SWC, the transpiler used to post-process the JavaScript produced by the Kotlin compiler.
 *
 * @see KotlinToolchainService
 */
@ExperimentalKotlinToolchainDsl
interface SwcToolchainService<P : KotlinToolchainService.Parameters> : KotlinToolchainService<P>

/**
 * The default [SwcToolchainService] used by the Kotlin Gradle Plugin.
 *
 * Downloads the requested SWC distribution and installs it into a shared, machine-wide directory,
 * so that a distribution is downloaded once and then reused by all builds on the machine.
 */
@ExperimentalKotlinToolchainDsl
interface DefaultSwcToolchainService : SwcToolchainService<DownloadedToolchainParameters>

/**
 * A [SwcToolchainService] that uses a pre-installed SWC instead of downloading one.
 *
 * The SWC installation is not provisioned; it must be available on the machine that runs the build.
 */
@ExperimentalKotlinToolchainDsl
interface PreInstalledSwcToolchainService : SwcToolchainService<PreInstalledToolchainParameters>

/**
 * A [SwcToolchainService] that disables SWC toolchain provisioning.
 *
 * Without an SWC toolchain, SWC is configured and provisioned per project, using the SWC environment spec
 * and the SWC setup task, instead of the build-wide toolchain service.
 *
 * Selected with [SwcToolchainManagementDsl.disable].
 */
@ExperimentalKotlinToolchainDsl
interface DisabledSwcToolchainService : SwcToolchainService<KotlinToolchainService.Parameters>

/**
 * `swc { ... }` block of the `toolchainManagement { ... }` DSL.
 *
 * ```
 * // settings.gradle.kts
 * kotlin {
 *     toolchainManagement {
 *         swc {
 *             toolchainService {
 *                 downloadBaseUrl.set("https://my.mirror.example/swc/releases/download")
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
interface SwcToolchainManagementDsl : ToolchainManagementDsl
