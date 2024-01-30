/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package org.jetbrains.kotlin.backend.konan

// Must match `RuntimeAssertsMode` in CompilerConstants.hpp
enum class GCBarriersCodegenMode(val value: Int) {
    BRANCH(0),
    CALL(1),
    BRANCH_CALL(2),
}
