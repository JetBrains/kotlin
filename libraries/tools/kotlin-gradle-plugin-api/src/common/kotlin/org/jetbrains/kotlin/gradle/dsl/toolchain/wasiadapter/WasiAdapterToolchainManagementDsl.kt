/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dsl.toolchain.wasiadapter

import org.gradle.api.Action
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import kotlin.reflect.KClass

/**
 * DSL for selecting and configuring the [WasiAdapterToolchainService] used by the whole build, exposed as
 * `toolchainManagement { wasiAdapter { ... } }` in `settings.gradle.kts` by the `org.jetbrains.kotlin.ecosystem` plugin.
 *
 * At most one of [toolchainService] or [disable] may be called - the [WasiAdapterToolchainService] is registered as a
 * shared [org.gradle.api.services.BuildService] right away, under the same name every Kotlin Gradle Plugin project
 * looks up when it registers the toolchain service for itself, so calling this more than once would silently keep
 * only the first registration.
 *
 * ```
 * kotlin {
 *     toolchainManagement {
 *         wasiAdapter {
 *             toolchainService(MyWasiAdapterToolchainService::class) { // specifying a service class is optional, DefaultWasiAdapterToolchainService will be used by default
 *             }
 *             toolchainService(DefaultWasiAdapterToolchainService::class) {
 *                 downloadBaseUrl("custom-url") // official Wasmtime releases on GitHub will be used by default
 *                 installationDir("custom location") // default <user.home>/.kotlin/toolchains/wasi-adapter location will be used by default
 *             }
 *             toolchainService(PreInstalledWasiAdapterToolchainService::class) {
 *                 adapterFile(file("path/to/wasi_snapshot_preview1.command.wasm"))
 *             }
 *         }
 *     }
 * }
 * ```
 *
 * **Note:** This interface is not intended for implementation by build script or plugin authors.
 */
@ExperimentalWasmDsl
interface WasiAdapterToolchainManagementDsl {

    /**
     * Configures the built-in, download-based [DefaultWasiAdapterToolchainService].
     *
     * This is the service used by the build when [toolchainService] and [disable] are never called.
     */
    @ExperimentalWasmDsl
    fun toolchainService(configure: DefaultWasiAdapterToolchainService.Parameters.() -> Unit)

    /**
     * Selects [serviceClass] as the [WasiAdapterToolchainService] used by the whole build, and configures its
     * [parameters][WasiAdapterToolchainService.Parameters].
     */
    @ExperimentalWasmDsl
    fun <P : WasiAdapterToolchainService.Parameters, T : WasiAdapterToolchainService<P>> toolchainService(
        serviceClass: KClass<T>,
        configure: P.() -> Unit,
    )

    /**
     * [Action] based version of [toolchainService] above.
     */
    @ExperimentalWasmDsl
    fun <P : WasiAdapterToolchainService.Parameters, T : WasiAdapterToolchainService<P>> toolchainService(
        serviceClass: KClass<T>,
        configure: Action<P>,
    ) {
        toolchainService(serviceClass) {
            configure.execute(this)
        }
    }

    /**
     * Disables WASI adapter provisioning for the whole build - tasks that need a WASI adapter will fail.
     *
     * A shorthand for `toolchainService(DisabledWasiAdapterToolchainService::class) { }`.
     */
    @ExperimentalWasmDsl
    fun disable() {
        toolchainService(DisabledWasiAdapterToolchainService::class) { }
    }
}
