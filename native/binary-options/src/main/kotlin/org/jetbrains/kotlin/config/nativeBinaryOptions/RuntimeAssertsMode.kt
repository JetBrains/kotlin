/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.config.nativeBinaryOptions

// Must match `RuntimeAssertsMode` in CompilerConstants.hpp
enum class RuntimeAssertsMode(val value: Int) {
    IGNORE(0),
    LOG(1),
    PANIC(2),
}