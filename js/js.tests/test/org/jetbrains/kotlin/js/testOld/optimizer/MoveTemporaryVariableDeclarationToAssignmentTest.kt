/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.testOld.optimizer

import org.junit.Test

class MoveTemporaryVariableDeclarationToAssignmentTest : BasicOptimizerTest("move-temporary-variable-declaration") {
    @Test
    fun sameBlock() = box()

    @Test
    fun innerBlock() = box()

    @Test
    fun siblingBlocks() = box()

    @Test
    fun notInitUsage() = box()
}
