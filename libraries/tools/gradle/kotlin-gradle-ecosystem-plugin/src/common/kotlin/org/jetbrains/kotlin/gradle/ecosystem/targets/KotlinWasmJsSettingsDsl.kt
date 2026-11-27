/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.ecosystem.targets

import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.jetbrains.kotlin.gradle.ExperimentalNpmDependencyManagementDsl
import org.jetbrains.kotlin.gradle.ecosystem.KotlinEcosystemExtension
import org.jetbrains.kotlin.gradle.ecosystem.npm.WasmJsNpmDependencyManagementDsl
import javax.inject.Inject

/**
 * `wasmJs { ... }` block of [KotlinEcosystemExtension].
 *
 * Configures build-wide settings of the Kotlin/Wasm JS targets.
 *
 * @since 2.5.20
 */
abstract class KotlinWasmJsSettingsDsl @Inject internal constructor(
    objects: ObjectFactory,
) {

    /**
     * Configures how the npm dependencies of the Kotlin/Wasm JS targets of the build are installed.
     *
     * @see WasmJsNpmDependencyManagementDsl
     * @since 2.5.20
     */
    @ExperimentalNpmDependencyManagementDsl
    val npmDependencyManagement: WasmJsNpmDependencyManagementDsl =
        objects.newInstance(WasmJsNpmDependencyManagementDsl::class.java)

    /**
     * Configures how the npm dependencies of the Kotlin/Wasm JS targets of the build are installed.
     *
     * @see WasmJsNpmDependencyManagementDsl
     * @since 2.5.20
     */
    @ExperimentalNpmDependencyManagementDsl
    fun npmDependencyManagement(configure: WasmJsNpmDependencyManagementDsl.() -> Unit) {
        npmDependencyManagement.configure()
    }

    /**
     * [Action] based version of [npmDependencyManagement] above.
     *
     * @see WasmJsNpmDependencyManagementDsl
     * @since 2.5.20
     */
    @ExperimentalNpmDependencyManagementDsl
    fun npmDependencyManagement(configure: Action<WasmJsNpmDependencyManagementDsl>) {
        npmDependencyManagement {
            configure.execute(this)
        }
    }
}
