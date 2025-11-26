/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.js.config

enum class ModuleKind(
    val jsExtension: String,
    val dtsExtension: String,
    val type: String
) {
    PLAIN(".js", ".d.ts", "plain"),
    AMD(".js", ".d.ts", "amd"),
    COMMON_JS(".js", ".d.ts", "commonjs"),
    UMD(".js", ".d.ts", "umd"),
    ES(".mjs", ".d.mts", "es");

    companion object {
        private val moduleMap = entries.associateBy(ModuleKind::type)

        @JvmStatic
        fun fromType(type: String) = moduleMap[type] ?: error("Unknown module type: $type")
    }
}
