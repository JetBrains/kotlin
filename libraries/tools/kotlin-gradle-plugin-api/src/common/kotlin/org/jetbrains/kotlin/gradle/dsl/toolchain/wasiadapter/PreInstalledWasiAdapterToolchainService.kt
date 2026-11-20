/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dsl.toolchain.wasiadapter

import org.gradle.api.file.RegularFileProperty
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

/**
 * A [WasiAdapterToolchainService] that uses an explicit, pre-installed WASI preview1 adapter module instead of downloading one.
 *
 * Configure it via [Parameters].
 */
@ExperimentalWasmDsl
interface PreInstalledWasiAdapterToolchainService : WasiAdapterToolchainService<PreInstalledWasiAdapterToolchainService.Parameters> {

    /**
     * Parameters of [PreInstalledWasiAdapterToolchainService], specifying which already installed WASI adapter file to use.
     */
    abstract class Parameters : WasiAdapterToolchainService.Parameters {

        /**
         * The adapter module to use. Required - nothing is downloaded in this mode.
         */
        abstract val adapterFile: RegularFileProperty
    }
}
