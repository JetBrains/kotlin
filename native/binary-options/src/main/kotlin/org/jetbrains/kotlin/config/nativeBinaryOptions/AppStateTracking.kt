/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.config.nativeBinaryOptions

/**
 * Controls whether K/N runtime is allowed track application state.
 *
 * Can be turned off via [BinaryOptions] to workaround bugs in implementation.
 */
// Must match `AppStateTracking` in CompilerConstants.hpp
enum class AppStateTracking(val value: Int) {
    DISABLED(0),
    ENABLED(1),
}