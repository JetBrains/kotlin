/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.wasm.wasiadapter.toolchain

import org.gradle.api.invocation.Gradle
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.toolchain.wasiadapter.DisabledWasiAdapterToolchainService
import org.jetbrains.kotlin.gradle.dsl.toolchain.wasiadapter.WasiAdapter
import org.jetbrains.kotlin.gradle.dsl.toolchain.wasiadapter.WasiAdapterRequest
import org.jetbrains.kotlin.gradle.dsl.toolchain.wasiadapter.WasiAdapterToolchainService
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_WASM_WASI_ADAPTER_TOOLCHAIN_MODE

@OptIn(ExperimentalWasmDsl::class)
internal abstract class DisabledWasiAdapterToolchainServiceImpl : DisabledWasiAdapterToolchainService {

    override fun requestAdapter(adapterRequest: WasiAdapterRequest): Provider<WasiAdapter> {
        throw UnsupportedOperationException(
            "WASI adapter toolchain is disabled. " +
                    "To enable it, remove 'disable()' from 'toolchainManagement { wasiAdapter { ... } }' or set '${KOTLIN_WASM_WASI_ADAPTER_TOOLCHAIN_MODE}=${WasiAdapterToolchainMode.DOWNLOAD.name}'."
        )
    }

    companion object {
        internal fun registerIfAbsent(gradle: Gradle): Provider<out WasiAdapterToolchainService<out WasiAdapterToolchainService.Parameters>> {
            return gradle.sharedServices.registerIfAbsent(wasiAdapterServiceName, DisabledWasiAdapterToolchainServiceImpl::class.java)
        }
    }
}
