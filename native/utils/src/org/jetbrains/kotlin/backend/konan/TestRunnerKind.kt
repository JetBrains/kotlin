/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

enum class TestRunnerKind {
    NONE,
    MAIN_THREAD,
    WORKER,
    MAIN_THREAD_NO_EXIT
}
