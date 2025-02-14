/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.platform.wasm

enum class WasmMultimoduleMode(val alias: String) {
    NONE("none"),
    MASTER("master"),
    SLAVE("slave");

    companion object {
        fun fromName(name: String): WasmMultimoduleMode? = WasmMultimoduleMode.entries.firstOrNull { it.alias == name }
    }
}