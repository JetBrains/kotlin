/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.ecosystem.internal

import org.jetbrains.kotlin.gradle.ExperimentalNodeJsToolchainDsl
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.toolchain.nodejs.NodeJsToolchainManagementDsl
import org.jetbrains.kotlin.gradle.dsl.toolchain.wasiadapter.WasiAdapterToolchainManagementDsl
import org.jetbrains.kotlin.gradle.dsl.toolchain.wasmtools.WasmToolsToolchainManagementDsl
import org.jetbrains.kotlin.gradle.ecosystem.KotlinToolchainManagementDsl

@OptIn(ExperimentalWasmDsl::class)
internal class KotlinToolchainManagementDslImpl(
    private val nodeJsDsl: NodeJsToolchainManagementDsl,
    private val wasiAdapterDsl: WasiAdapterToolchainManagementDsl,
    private val wasmToolsDsl: WasmToolsToolchainManagementDsl,
) : KotlinToolchainManagementDsl {

    @ExperimentalNodeJsToolchainDsl
    override fun nodeJs(configure: NodeJsToolchainManagementDsl.() -> Unit) {
        nodeJsDsl.configure()
    }

    @ExperimentalWasmDsl
    override fun wasiAdapter(configure: WasiAdapterToolchainManagementDsl.() -> Unit) {
        wasiAdapterDsl.configure()
    }

    @ExperimentalWasmDsl
    override fun wasmTools(configure: WasmToolsToolchainManagementDsl.() -> Unit) {
        wasmToolsDsl.configure()
    }
}
