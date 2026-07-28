/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.llvm

// Must be synchronized with RuntimePrivate.hpp
enum class StaticInitState(val value: Int) {
    FILE_NOT_INITIALIZED(0),
    FILE_BEING_INITIALIZED(1),
    FILE_INITIALIZED(2),
    FILE_FAILED_TO_INITIALIZE(3),
}