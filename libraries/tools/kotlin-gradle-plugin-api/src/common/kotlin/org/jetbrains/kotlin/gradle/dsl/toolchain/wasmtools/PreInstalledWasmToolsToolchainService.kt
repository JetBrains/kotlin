/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dsl.toolchain.wasmtools

import org.gradle.api.provider.Property
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

/**
 * A [WasmToolsToolchainService] that uses a pre-installed wasm-tools instead of downloading one.
 *
 * The wasm-tools installation is not provisioned;
 * it must be available on the machine that runs the build.
 * Configure it via [Parameters].
 */
@ExperimentalWasmDsl
interface PreInstalledWasmToolsToolchainService : WasmToolsToolchainService<PreInstalledWasmToolsToolchainService.Parameters> {

    /**
     * Parameters of [PreInstalledWasmToolsToolchainService], specifying which already installed wasm-tools to run.
     * If [wasmToolsExecutable] is left unset, `wasm-tools` from the `PATH` is used.
     */
    abstract class Parameters : WasmToolsToolchainService.Parameters {
        /**
         * The command used to run the pre-installed wasm-tools, for example `wasm-tools` or a full path.
         */
        abstract val wasmToolsExecutable: Property<String>
    }
}
