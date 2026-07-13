/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.wasm

internal enum class WasmCompilationMode {
    MONOLITH,
    MULTIMODULE_OPEN_WORLD,
    MULTIMODULE_CLOSED_WORLD,
    MULTIMODULE_CLOSED_WORLD_ONLY_IN_DEV;

    internal fun isOpenWorld() = this == MULTIMODULE_OPEN_WORLD

    companion object {
        fun byArgument(argument: String): WasmCompilationMode? =
            WasmCompilationMode.values()
                .firstOrNull { it.name.replace("_", "-").equals(argument, ignoreCase = true) }

        fun WasmCompilationMode.toArgument(): String = name.lowercase().replace("_", "-")
    }
}
