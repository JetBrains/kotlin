/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.js.config

enum class ModuleKind(val jsExtension: String, val dtsExtension: String) {
    PLAIN(".js", ".d.ts"),
    AMD(".js", ".d.ts"),
    COMMON_JS(".js", ".d.ts"),
    UMD(".js", ".d.ts"),
    ES(".mjs", ".d.mts")
}
