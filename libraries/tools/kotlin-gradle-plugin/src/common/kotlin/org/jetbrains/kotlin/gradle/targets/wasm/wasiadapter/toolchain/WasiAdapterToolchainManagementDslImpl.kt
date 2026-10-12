/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.wasm.wasiadapter.toolchain

import org.gradle.api.invocation.Gradle
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.toolchain.wasiadapter.DefaultWasiAdapterToolchainService
import org.jetbrains.kotlin.gradle.dsl.toolchain.wasiadapter.WasiAdapterToolchainManagementDsl
import org.jetbrains.kotlin.gradle.dsl.toolchain.wasiadapter.WasiAdapterToolchainService
import kotlin.reflect.KClass

@ExperimentalWasmDsl
@InternalKotlinGradlePluginApi
class WasiAdapterToolchainManagementDslImpl(
    private val gradle: Gradle,
) : WasiAdapterToolchainManagementDsl {

    private var configured = false

    override fun toolchainService(configure: DefaultWasiAdapterToolchainService.Parameters.() -> Unit) {
        toolchainService(DefaultWasiAdapterToolchainService::class) {
            configure()
        }
    }

    override fun <P : WasiAdapterToolchainService.Parameters, T : WasiAdapterToolchainService<P>> toolchainService(
        serviceClass: KClass<T>,
        configure: P.() -> Unit,
    ) {
        check(!configured) {
            "The WASI adapter toolchain service is already configured. " +
                "Only one 'toolchainService(...)' or 'disable()' call is allowed inside 'toolchainManagement { wasiAdapter { ... } }'."
        }
        configured = true

        registerWasiAdapterToolchainServiceIfAbsent(gradle, serviceClass.java, configure)
    }
}
