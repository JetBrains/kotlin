/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalWasmDsl::class)

package org.jetbrains.kotlin.gradle.ecosystem

import org.gradle.api.Action
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalNodeJsToolchainDsl
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.KotlinNativeSettingsDsl
import org.jetbrains.kotlin.gradle.ecosystem.targets.KotlinJsSettingsDsl
import org.jetbrains.kotlin.gradle.ecosystem.targets.KotlinWasmJsSettingsDsl

/**
 * Entry point of the `org.jetbrains.kotlin.ecosystem` settings plugin DSL, exposed as `kotlin { ... }`
 * in `settings.gradle.kts`.
 *
 * ```
 * kotlin {
 *     toolchainManagement {
 *         nodeJs {
 *             toolchainService { ... }
 *         }
 *     }
 *     native {
 *         linkMaxWorkers = 2
 *     }
 * }
 * ```
 *
 * @since 2.5.0
 */
interface KotlinEcosystemExtension {

    /**
     * Configures build-wide Kotlin/Native settings. See [KotlinNativeSettingsDsl].
     *
     * @since 2.5.20
     */
    @ExperimentalKotlinGradlePluginApi
    fun native(configure: KotlinNativeSettingsDsl.() -> Unit)

    /**
     * [Action] based version of [native] above.
     *
     * @since 2.5.20
     */
    @ExperimentalKotlinGradlePluginApi
    fun native(configure: Action<KotlinNativeSettingsDsl>) {
        native {
            configure.execute(this)
        }
    }

    /**
     * Configures toolchains provisioned for the whole build.
     *
     * @since 2.5.0
     */
    @ExperimentalNodeJsToolchainDsl
    fun toolchainManagement(configure: KotlinToolchainManagementDsl.() -> Unit)

    /**
     * [Action] based version of [toolchainManagement] above.
     *
     * @since 2.5.0
     */
    @ExperimentalNodeJsToolchainDsl
    fun toolchainManagement(configure: Action<KotlinToolchainManagementDsl>) {
        toolchainManagement {
            configure.execute(this)
        }
    }

    /**
     * Configures build-wide Kotlin/JS settings.
     *
     * @see KotlinJsSettingsDsl
     * @since 2.5.20
     */
    @ExperimentalKotlinGradlePluginApi
    val js: KotlinJsSettingsDsl

    /**
     * Configures build-wide Kotlin/JS settings.
     *
     * @see KotlinJsSettingsDsl
     * @since 2.5.20
     */
    @ExperimentalKotlinGradlePluginApi
    fun js(configure: KotlinJsSettingsDsl.() -> Unit)

    /**
     * [Action] based version of [js] above.
     *
     * @see KotlinJsSettingsDsl
     * @since 2.5.20
     */
    @ExperimentalKotlinGradlePluginApi
    fun js(configure: Action<KotlinJsSettingsDsl>) {
        js {
            configure.execute(this)
        }
    }

    /**
     * Configures build-wide Kotlin/Wasm JS settings.
     *
     * @see KotlinWasmJsSettingsDsl
     * @since 2.5.20
     */
    @ExperimentalWasmDsl
    val wasmJs: KotlinWasmJsSettingsDsl

    /**
     * Configures build-wide Kotlin/Wasm JS settings.
     *
     * @see KotlinWasmJsSettingsDsl
     * @since 2.5.20
     */
    @ExperimentalWasmDsl
    fun wasmJs(configure: KotlinWasmJsSettingsDsl.() -> Unit)

    /**
     * [Action] based version of [wasmJs] above.
     *
     * @see KotlinWasmJsSettingsDsl
     * @since 2.5.20
     */
    @ExperimentalWasmDsl
    fun wasmJs(configure: Action<KotlinWasmJsSettingsDsl>) {
        wasmJs {
            configure.execute(this)
        }
    }
}
