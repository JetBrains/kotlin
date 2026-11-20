/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dsl.toolchain.wasiadapter

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

/**
 * A [WasiAdapterToolchainService] that disables WASI adapter toolchain provisioning.
 *
 * Selected with [WasiAdapterToolchainManagementDsl.disable].
 */
@ExperimentalWasmDsl
interface DisabledWasiAdapterToolchainService : WasiAdapterToolchainService<WasiAdapterToolchainService.Parameters>
