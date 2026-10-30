/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks.toolchain

import org.gradle.api.Task
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.jetbrains.kotlin.gradle.ExperimentalKotlinToolchainDsl
import org.jetbrains.kotlin.gradle.dsl.toolchain.binaryen.BinaryenToolchainService
import org.jetbrains.kotlin.gradle.dsl.toolchain.d8.D8ToolchainService
import org.jetbrains.kotlin.gradle.dsl.toolchain.swc.SwcToolchainService
import org.jetbrains.kotlin.gradle.dsl.toolchain.wasmtime.WasmtimeToolchainService

/**
 * A [Task] that uses a [D8ToolchainService] to provision D8.
 *
 * The Kotlin Gradle Plugin sets [d8ToolchainService] on every [UsesD8ToolchainService] task.
 */
@ExperimentalKotlinToolchainDsl
interface UsesD8ToolchainService : Task {
    /**
     * The [D8ToolchainService] used for D8 provisioning.
     */
    @get:Internal
    val d8ToolchainService: Property<D8ToolchainService<*>>
}

/**
 * A [Task] that uses a [BinaryenToolchainService] to provision Binaryen.
 *
 * The Kotlin Gradle Plugin sets [binaryenToolchainService] on every [UsesBinaryenToolchainService] task.
 */
@ExperimentalKotlinToolchainDsl
interface UsesBinaryenToolchainService : Task {
    /**
     * The [BinaryenToolchainService] used for Binaryen provisioning.
     */
    @get:Internal
    val binaryenToolchainService: Property<BinaryenToolchainService<*>>
}

/**
 * A [Task] that uses a [WasmtimeToolchainService] to provision Wasmtime.
 *
 * The Kotlin Gradle Plugin sets [wasmtimeToolchainService] on every [UsesWasmtimeToolchainService] task.
 */
@ExperimentalKotlinToolchainDsl
interface UsesWasmtimeToolchainService : Task {
    /**
     * The [WasmtimeToolchainService] used for Wasmtime provisioning.
     */
    @get:Internal
    val wasmtimeToolchainService: Property<WasmtimeToolchainService<*>>
}

/**
 * A [Task] that uses a [SwcToolchainService] to provision SWC.
 *
 * The Kotlin Gradle Plugin sets [swcToolchainService] on every [UsesSwcToolchainService] task.
 */
@ExperimentalKotlinToolchainDsl
interface UsesSwcToolchainService : Task {
    /**
     * The [SwcToolchainService] used for SWC provisioning.
     */
    @get:Internal
    val swcToolchainService: Property<SwcToolchainService<*>>
}
