/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.wasm.wasmtools.toolchain

import org.gradle.api.invocation.Gradle
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.toolchain.wasmtools.DefaultWasmToolsToolchainService
import org.jetbrains.kotlin.gradle.dsl.toolchain.wasmtools.WasmToolsToolchainManagementDsl
import org.jetbrains.kotlin.gradle.dsl.toolchain.wasmtools.WasmToolsToolchainService
import kotlin.reflect.KClass

@ExperimentalWasmDsl
@InternalKotlinGradlePluginApi
class WasmToolsToolchainManagementDslImpl(
    private val gradle: Gradle,
) : WasmToolsToolchainManagementDsl {

    private var configured = false

    override fun toolchainService(configure: DefaultWasmToolsToolchainService.Parameters.() -> Unit) {
        toolchainService(DefaultWasmToolsToolchainServiceImpl::class) {
            offline.set(gradle.startParameter.isOffline)
            configure()
        }
    }

    override fun <P : WasmToolsToolchainService.Parameters, T : WasmToolsToolchainService<P>> toolchainService(
        serviceClass: KClass<T>,
        configure: P.() -> Unit,
    ) {
        check(!configured) {
            "The wasm-tools toolchain service is already configured. " +
                    "Only one 'toolchainService(...)' or 'disable()' call is allowed inside 'toolchainManagement { wasmTools { ... } }'."
        }
        configured = true

        registerWasmToolsToolchainServiceIfAbsent(gradle, serviceClass.java, configure)
    }
}
