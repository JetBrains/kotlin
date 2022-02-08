/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package org.jetbrains.kotlin.backend.konan

// Must match `WorkerExceptionHandling` in CompilerConstants.hpp
enum class WorkerExceptionHandling(val value: Int) {
    LEGACY(0),
    USE_HOOK(1),
}
