/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dsl.toolchain.wasmtools

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

/**
 * The default [WasmToolsToolchainService] used by the Kotlin Gradle Plugin.
 *
 * Downloads the requested wasm-tools distribution and installs it into a shared, machine-wide directory,
 * so that a distribution is downloaded once and then reused by all builds on the machine.
 */
@ExperimentalWasmDsl
interface DefaultWasmToolsToolchainService : WasmToolsToolchainService<DefaultWasmToolsToolchainService.Parameters> {

    /**
     * Parameters of [DefaultWasmToolsToolchainService], controlling where wasm-tools distributions are
     * downloaded from and where they are installed.
     */
    abstract class Parameters : WasmToolsToolchainService.Parameters {
        /**
         * The directory containing all wasm-tools installations.
         *
         * Defaults to `<user.home>/.kotlin/toolchains/wasm-tools`.
         */
        abstract val installationDir: DirectoryProperty

        /**
         * The base URL the distributions are downloaded from.
         *
         * Defaults to the official wasm-tools distribution, `https://github.com/bytecodealliance/wasm-tools/releases/download`.
         */
        abstract val downloadBaseUrl: Property<String>

        /**
         * Whether the build is running in offline mode. It is set with Gradle --offline option.
         *
         * When `true`, an already installed distribution is reused, and a missing one fails the build
         * instead of being downloaded.
         */
        abstract val offline: Property<Boolean>
    }
}
