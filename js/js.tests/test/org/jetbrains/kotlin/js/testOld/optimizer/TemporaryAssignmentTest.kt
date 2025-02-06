/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.testOld.optimizer

import org.junit.Test

class TemporaryAssignmentTest : BasicOptimizerTest("temporary-assignment") {
    @Test fun assignment() = box()

    @Test fun returnStatement() = box()

    @Test fun declaration() = box()

    @Test fun skipsGlobalDeclarations() = box()

    @Test fun transitiveAssignment() = box()

    @Test fun transitiveChain() = box()

    @Test fun tryCatch() = box()

    @Test fun ifWithoutElse() = box()

    @Test fun forInitVariables() = box()

    @Test fun forInitExpressionPreventsOptimization() = box()
}
