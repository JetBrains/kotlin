/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

// Must match `GCSchedulerType` in CompilerConstants.hpp
enum class GCSchedulerType(val value: Int, val deprecatedWithReplacement: GCSchedulerType? = null) {
    MANUAL(0),
    ADAPTIVE(1),
    AGGRESSIVE(3),
    // Deprecated:
    DISABLED(0, deprecatedWithReplacement = MANUAL),
    WITH_TIMER(1, deprecatedWithReplacement = ADAPTIVE),
    ON_SAFE_POINTS(2, deprecatedWithReplacement = ADAPTIVE),
}
