/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks.wasmtools

import org.gradle.api.Task
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.toolchain.wasmtools.WasmToolsToolchainService

/**
 * A [Task] that uses a [WasmToolsToolchainService] to provision wasm-tools.
 *
 * The Kotlin Gradle Plugin will set [wasmToolsToolchainService] to the [UsesWasmToolsToolchainService] tasks.
 */
@ExperimentalWasmDsl
interface UsesWasmToolsToolchainService : Task {
    /**
     * The [WasmToolsToolchainService] is used for wasm-tools provisioning.
     */
    @get:Internal
    val wasmToolsToolchainService: Property<WasmToolsToolchainService<*>>
}
