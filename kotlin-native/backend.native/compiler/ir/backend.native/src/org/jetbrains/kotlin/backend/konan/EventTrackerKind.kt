/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package org.jetbrains.kotlin.backend.konan

// Must match `EventKind` in profiler/ProfilerEvents.hpp
enum class EventTrackerKind(val ord: Int, val defaultBacktraceDepth: Int) {
    Allocation(0, 2),
    SafePoint(1, 2),
    SpecialRef(2, 2),
    ;

    companion object {
        fun parse(str: String) = entries.firstOrNull {
            it.name.equals(str, ignoreCase = true)
        }
    }
}
