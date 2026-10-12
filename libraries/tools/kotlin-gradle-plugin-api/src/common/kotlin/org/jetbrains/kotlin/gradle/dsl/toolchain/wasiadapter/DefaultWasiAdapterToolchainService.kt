/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dsl.toolchain.wasiadapter

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

/**
 * The default [WasiAdapterToolchainService] used by the Kotlin Gradle Plugin.
 *
 * Downloads the requested WASI preview1 adapter and installs it into a shared, machine-wide directory,
 * so that an artifact is downloaded once and then reused by all builds on the machine.
 */
@ExperimentalWasmDsl
interface DefaultWasiAdapterToolchainService : WasiAdapterToolchainService<DefaultWasiAdapterToolchainService.Parameters> {

    /**
     * Parameters of [DefaultWasiAdapterToolchainService], controlling where WASI preview1 adapters are
     * downloaded from and where they are installed.
     */
    abstract class Parameters : WasiAdapterToolchainService.Parameters {

        /**
         * The directory containing all WASI adapter installations.
         *
         * Defaults to `<user.home>/.kotlin/toolchains/wasi-adapter`.
         */
        abstract val installationDir: DirectoryProperty

        /**
         * The base URL the adapters are downloaded from.
         *
         * Defaults to official Wasmtime releases on GitHub: `https://github.com/bytecodealliance/wasmtime/releases/download`.
         */
        abstract val downloadBaseUrl: Property<String>

        /**
         * Whether the build is running in offline mode. It is set with Gradle --offline option.
         *
         * When `true`, an already installed adapter is reused, and a missing one fails the build
         * instead of being downloaded.
         */
        abstract val offline: Property<Boolean>
    }
}
