/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package org.jetbrains.kotlin.backend.konan

// Must match `RuntimeAssertsMode` in CompilerConstants.hpp
enum class RuntimeAssertsMode(val value: Int) {
    IGNORE(0),
    LOG(1),
    PANIC(2),
}
