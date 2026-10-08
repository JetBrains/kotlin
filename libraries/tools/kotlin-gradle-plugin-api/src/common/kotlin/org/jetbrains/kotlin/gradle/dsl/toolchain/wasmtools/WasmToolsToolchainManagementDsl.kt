/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dsl.toolchain.wasmtools

import org.gradle.api.Action
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import kotlin.reflect.KClass

/**
 * DSL for selecting and configuring the [WasmToolsToolchainService] used by the whole build, exposed as
 * `toolchainManagement { wasmTools { ... } }` in `settings.gradle.kts` by the `org.jetbrains.kotlin.ecosystem` plugin.
 *
 * At most one of [toolchainService] or [disable] may be called - the [WasmToolsToolchainService] is registered as a
 * shared [org.gradle.api.services.BuildService] right away, under the same name every Kotlin Gradle Plugin project
 * looks up when it registers the toolchain service for itself, so calling this more than once would silently keep
 * only the first registration.
 *
 * ```
 * kotlin {
 *     toolchainManagement {
 *         wasmTools {
 *             toolchainService(MyWasmToolsToolchainService::class) { //specify a service class is optional, DefaultWasmToolsToolchainService will be used by default
 *             }
 *             toolchainService(DefaultWasmToolsToolchainService::class) {
 *                 downloadBaseUrl("custom-url") //official wasm-tools website will be used by default
 *                 installationDir("custom location") //default <user.home>/.kotlin/toolchains/wasm-tools location will be used by default
 *             }
 *             toolchainService(PreInstalledWasmToolsToolchainService::class) {
 *                 wasmToolsExecutable("path_to_wasm_tools_executable") //"wasm-tools" will be used by default
 *             }
 *        }
 *     }
 * }
 * ```
 *
 * **Note:** This interface is not intended for implementation by build script or plugin authors.
 */
interface WasmToolsToolchainManagementDsl {
    /**
     * Configures the built-in, download-based [DefaultWasmToolsToolchainService].
     *
     * This is the service used by the build when [toolchainService] and [disable] are never called.
     */
    @ExperimentalWasmDsl
    fun toolchainService(configure: DefaultWasmToolsToolchainService.Parameters.() -> Unit)

    /**
     * Selects [serviceClass] as the [WasmToolsToolchainService] used by the whole build, and configures its
     * [parameters][WasmToolsToolchainService.Parameters].
     */
    @ExperimentalWasmDsl
    fun <P : WasmToolsToolchainService.Parameters, T : WasmToolsToolchainService<P>> toolchainService(
        serviceClass: KClass<T>,
        configure: P.() -> Unit,
    )

    /**
     * [Action] based version of [toolchainService] above.
     */
    @ExperimentalWasmDsl
    fun <P : WasmToolsToolchainService.Parameters, T : WasmToolsToolchainService<P>> toolchainService(
        serviceClass: KClass<T>,
        configure: Action<P>,
    ) {
        toolchainService(serviceClass) {
            configure.execute(this)
        }
    }

    /**
     * Disables wasm-tools provisioning for the whole build - tasks that need a wasm-tools executable will fail.
     *
     * A shorthand for `toolchainService(DisabledWasmToolsToolchainService::class) { }`.
     */
    @ExperimentalWasmDsl
    fun disable() {
        toolchainService(DisabledWasmToolsToolchainService::class) { }
    }
}
