/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks.wasiadapter

import org.gradle.api.Task
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.toolchain.wasiadapter.WasiAdapterToolchainService

/**
 * A [Task] that uses a [WasiAdapterToolchainService] to provision WASI preview1 adapters.
 *
 * The Kotlin Gradle Plugin will set [wasiAdapterToolchainService] to the [UsesWasiAdapterToolchainService] tasks.
 */
@ExperimentalWasmDsl
interface UsesWasiAdapterToolchainService : Task {

    /**
     * The [WasiAdapterToolchainService] used for WASI preview1 adapter provisioning.
     */
    @get:Internal
    val wasiAdapterToolchainService: Property<WasiAdapterToolchainService<*>>
}
