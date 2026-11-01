/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dsl.toolchain.binaryen

import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.jetbrains.kotlin.gradle.ExperimentalExternalToolchainDsl

/**
 * Provisions Binaryen, the WebAssembly optimizer (`wasm-opt`) used to optimize Kotlin/Wasm binaries.
 *
 * Build authors may implement this interface to supply Binaryen from their own distribution source.
 * One implementation is shared by the tasks of the build.
 */
@ExperimentalExternalToolchainDsl
interface BinaryenToolchainService<P : BuildServiceParameters> : BuildService<P> {
    /** Resolves the requested distribution when the returned provider is queried, at task execution time. */
    fun request(request: BinaryenToolchainRequest): Provider<ProvisionedBinaryenToolchain>
}
