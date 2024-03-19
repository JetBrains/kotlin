/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.platform.wasm

enum class WasmTarget(val alias: String) {
    JS("wasm-js"),
    WASI("wasm-wasi");

    companion object {
        fun fromName(name: String): WasmTarget? = WasmTarget.entries.firstOrNull { it.alias == name }
    }
}