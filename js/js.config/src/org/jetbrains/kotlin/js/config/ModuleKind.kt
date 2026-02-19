/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.js.config

enum class ModuleKind(
    val jsExtension: String,
    val tsExtension: String,
    val type: String
) {
    PLAIN(".js", ".ts", "plain"),
    AMD(".js", ".ts", "amd"),
    COMMON_JS(".js", ".ts", "commonjs"),
    UMD(".js", ".ts", "umd"),
    ES(".mjs", ".mts", "es");

    val dtsExtension: String
        get() = ".d$tsExtension"

    companion object {
        private val moduleMap = entries.associateBy(ModuleKind::type)

        val allowedJsExtensions: Set<String> = entries.mapTo(mutableSetOf()) {
            it.jsExtension.removePrefix(".")
        }

        @JvmStatic
        fun fromType(type: String) = moduleMap[type] ?: error("Unknown module type: $type")
    }
}
