/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dsl.toolchain.d8

import org.gradle.api.provider.Property
import org.jetbrains.kotlin.gradle.ExperimentalKotlinToolchainDsl
import org.jetbrains.kotlin.gradle.dsl.toolchain.DownloadedToolchainParameters
import org.jetbrains.kotlin.gradle.dsl.toolchain.ToolchainManagementDsl
import org.jetbrains.kotlin.gradle.dsl.toolchain.KotlinToolchainService
import org.jetbrains.kotlin.gradle.dsl.toolchain.PreInstalledToolchainParameters

/**
 * Provisions the D8 JavaScript and WebAssembly engine used to run Kotlin/Wasm applications and tests.
 *
 * @see KotlinToolchainService
 */
@ExperimentalKotlinToolchainDsl
interface D8ToolchainService<P : KotlinToolchainService.Parameters> : KotlinToolchainService<P>

/**
 * The default [D8ToolchainService] used by the Kotlin Gradle Plugin.
 *
 * Downloads the requested D8 distribution and installs it into a shared, machine-wide directory,
 * so that a distribution is downloaded once and then reused by all builds on the machine.
 */
@ExperimentalKotlinToolchainDsl
interface DefaultD8ToolchainService : D8ToolchainService<DefaultD8ToolchainService.Parameters> {

    /**
     * Parameters of [DefaultD8ToolchainService].
     */
    abstract class Parameters : DownloadedToolchainParameters() {

        /**
         * The edition of the D8 distribution.
         *
         * Valid options are `rel` (release variant) and `dbg` (debug variant). Defaults to `rel`.
         */
        abstract val edition: Property<String>
    }
}

/**
 * A [D8ToolchainService] that uses a pre-installed D8 instead of downloading one.
 *
 * The D8 installation is not provisioned; it must be available on the machine that runs the build.
 */
@ExperimentalKotlinToolchainDsl
interface PreInstalledD8ToolchainService : D8ToolchainService<PreInstalledToolchainParameters>

/**
 * A [D8ToolchainService] that disables D8 toolchain provisioning.
 *
 * Without a D8 toolchain, D8 is configured and provisioned per project, using the D8 environment spec
 * and the D8 setup task, instead of the build-wide toolchain service.
 *
 * Selected with [D8ToolchainManagementDsl.disable].
 */
@ExperimentalKotlinToolchainDsl
interface DisabledD8ToolchainService : D8ToolchainService<KotlinToolchainService.Parameters>

/**
 * `d8 { ... }` block of the `toolchainManagement { ... }` DSL.
 *
 * ```
 * // settings.gradle.kts
 * kotlin {
 *     toolchainManagement {
 *         d8 {
 *             toolchainService {
 *                 downloadBaseUrl.set("https://my.mirror.example/chromium-v8/official/canary")
 *                 installationDir.set(File("/opt/toolchains/d8"))
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
interface D8ToolchainManagementDsl : ToolchainManagementDsl
