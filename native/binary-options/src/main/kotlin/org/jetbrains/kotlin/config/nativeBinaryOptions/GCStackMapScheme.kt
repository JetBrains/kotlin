/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.config.nativeBinaryOptions

// Must match `GCStackMapScheme` in StackMapConfig.hpp
enum class GCStackMapScheme(val shortcut: String) {
    SHADOW_STACK("shadowstack"),
    DELTA_MAIN("delta-main"),
}
