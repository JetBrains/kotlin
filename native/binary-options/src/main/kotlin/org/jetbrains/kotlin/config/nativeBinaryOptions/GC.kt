/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.config.nativeBinaryOptions

enum class GC(val shortcut: String) {
    NOOP("noop"),
    STOP_THE_WORLD_MARK_AND_SWEEP("stwms"),
    PARALLEL_MARK_CONCURRENT_SWEEP("pmcs"),
    CONCURRENT_MARK_AND_SWEEP("cms"),
}