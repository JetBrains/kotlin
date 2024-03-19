/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.config

import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.platform.wasm.WasmTarget

val CompilerConfiguration.wasmTarget: WasmTarget
    get() = get(WasmConfigurationKeys.WASM_TARGET, /* defaultValue = */ WasmTarget.JS)