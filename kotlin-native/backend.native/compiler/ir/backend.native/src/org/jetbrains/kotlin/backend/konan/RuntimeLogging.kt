/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package org.jetbrains.kotlin.backend.konan

// Must match `Level` in Logging.hpp
enum class LoggingLevel(val ord: Int) {
    None(0), // marks disable logs, should not be used for other purposes
    Error(1),
    Warning(2),
    Info(3),
    Debug(4);

    companion object {
        fun parse(str: String) = LoggingLevel.entries.firstOrNull {
            it.name.equals(str, ignoreCase = true)
        }
    }
}

// Must match `Tag` in Logging.hpp
enum class LoggingTag(val ord: Int) {
    Logging(0),
    RT(1),
    GC(2),
    MM(3),
    TLS(4),
    Pause(5),
    Alloc(6),
    Balancing(7),
    Barriers(8),
    GCMark(9),
    ;

    companion object {
        fun parse(str: String) = entries.firstOrNull {
            it.name.equals(str, ignoreCase = true)
        }
    }
}
