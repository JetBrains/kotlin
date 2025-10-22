/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.dsl

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinD8Ir
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrSubTargetWithBinary
import org.jetbrains.kotlin.gradle.targets.wasm.d8.D8Exec
import org.jetbrains.kotlin.gradle.utils.withType

/**
 * Represents the Kotlin/Wasm target platform.
 *
 * **Note:** This interface is not intended for implementation by build script or plugin authors.
 */
interface KotlinWasmSubTargetContainerDsl : KotlinTarget {

    /**
     * Container for all execution environments enabled for this target.
     * Currently, the only supported environments are Node.js, browser, and D8.
     */
    // note: this should be annotated with @InternalKotlinGradlePluginApi
    val subTargets: NamedDomainObjectContainer<KotlinJsIrSubTargetWithBinary>

    /**
     * Returns the configuration options for D8 execution environment
     * used for this [KotlinTarget].
     *
     * @see org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinWasmD8Dsl
     */
    val d8: KotlinWasmD8Dsl

    /**
     * Internal property. It is not intended to be used by build script or plugin authors.
     *
     * Legacy method of detecting if a D8 execution environment is enabled.
     */
    val isD8Configured: Boolean
        get() = subTargets.withType<KotlinD8Ir>().isNotEmpty()

    /**
     * Applies configuration to all D8 execution environments used by this target.
     *
     * If D8 is not enabled for this target, [body] will not be used.
     *
     * @see org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinWasmD8Dsl
     */
    // note: this is a legacy function from before KGP fully migrated to the Provider API.
    fun whenD8Configured(body: KotlinWasmD8Dsl.() -> Unit) {
        subTargets
            .withType<KotlinD8Ir>()
            .configureEach(body)
    }
}

/**
 * Base configuration options for the compilation of Kotlin WasmJS targets.
 *
 * ```
 * kotlin {
 *     wasmJs { // Creates WasmJS target
 *         // Configure WasmJS target specifics here
 *     }
 * }
 * ```
 *
 * To learn more see:
 * - [Get started with Kotlin/Wasm and Compose Multiplatform](https://kotl.in/kotlin-wasm-js-setup).
 *
 * **Note:** This interface is not intended for implementation by build script or plugin authors.
 */
interface KotlinWasmJsTargetDsl : KotlinWasmTargetDsl, KotlinJsTargetDsl {

    /**
     * Enable d8 as the execution environment for this target
     *
     * When enabled, Kotlin Gradle plugin will download and install
     * the required environment and dependencies for running and testing
     * using d8.
     *
     * @see KotlinWasmD8Dsl
     */
    fun d8() = d8 { }

    /**
     * Enable d8 as the execution environment for this target
     *
     * When enabled, Kotlin Gradle plugin will download and install
     * the required environment and dependencies for running and testing
     * using d8.
     *
     * The target can be configured using [body].
     *
     * @see KotlinWasmD8Dsl
     */
    fun d8(body: KotlinWasmD8Dsl.() -> Unit)

    /**
     * [Action] based version of [d8] above.
     */
    fun d8(fn: Action<KotlinWasmD8Dsl>) {
        d8 {
            fn.execute(this)
        }
    }
}

/**
 * [d8](https://v8.dev/docs/d8) execution environment options for Kotlin WasmJS targets.
 *
 * **Note:** This interface is not intended for implementation by build script or plugin authors.
 */
@OptIn(ExperimentalWasmDsl::class)
interface KotlinWasmD8Dsl : KotlinJsSubTargetDsl {

    /**
     * Configure the default [D8Exec] task that **runs** the Kotlin WasmJS target.
     *
     * @see org.jetbrains.kotlin.gradle.targets.wasm.d8.D8Exec
     */
    fun runTask(body: D8Exec.() -> Unit) {
        runTask(Action {
            body(it)
        })
    }

    /**
     * [Action] based version of [runTask] above.
     */
    fun runTask(body: Action<D8Exec>)
}
