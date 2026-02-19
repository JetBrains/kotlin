/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.config.nativeBinaryOptions

// Must match `GCSchedulerType` in CompilerConstants.hpp
enum class GCSchedulerType(val deprecatedWithReplacement: GCSchedulerType? = null) {
    MANUAL,
    ADAPTIVE,
    AGGRESSIVE,
    // Deprecated:
    DISABLED(deprecatedWithReplacement = MANUAL),
    WITH_TIMER(deprecatedWithReplacement = ADAPTIVE),
    ON_SAFE_POINTS(deprecatedWithReplacement = ADAPTIVE),
}