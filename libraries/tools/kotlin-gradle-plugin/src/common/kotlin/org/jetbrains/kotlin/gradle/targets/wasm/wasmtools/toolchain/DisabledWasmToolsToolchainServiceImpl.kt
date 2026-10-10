/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.wasm.wasmtools.toolchain

import org.gradle.api.invocation.Gradle
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.toolchain.wasmtools.DisabledWasmToolsToolchainService
import org.jetbrains.kotlin.gradle.dsl.toolchain.wasmtools.WasmToolsExecutable
import org.jetbrains.kotlin.gradle.dsl.toolchain.wasmtools.WasmToolsRequest
import org.jetbrains.kotlin.gradle.dsl.toolchain.wasmtools.WasmToolsToolchainService

@OptIn(ExperimentalWasmDsl::class)
internal abstract class DisabledWasmToolsToolchainServiceImpl : DisabledWasmToolsToolchainService {

    override fun request(wasmToolsRequest: WasmToolsRequest): Provider<WasmToolsExecutable> {
        throw UnsupportedOperationException("wasm-tools toolchain is disabled")
    }

    companion object {
        internal fun registerIfAbsent(gradle: Gradle): Provider<out WasmToolsToolchainService<out WasmToolsToolchainService.Parameters>> {
            return gradle.sharedServices.registerIfAbsent(wasmToolsServiceName, DisabledWasmToolsToolchainServiceImpl::class.java)
        }
    }
}
