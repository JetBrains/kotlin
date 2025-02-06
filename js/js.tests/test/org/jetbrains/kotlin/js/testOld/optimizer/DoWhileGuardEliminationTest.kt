/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.testOld.optimizer

import org.junit.Test

class DoWhileGuardEliminationTest : BasicOptimizerTest("do-while-guard-elimination") {
    @Test fun simple() = box()

    @Test fun innerContinue() = box()

    @Test fun innerBreakInLoopWithoutLabel() = box()

    @Test fun emptyDoWhile() = box()
}