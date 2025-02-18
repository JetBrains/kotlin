/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.testOld.optimizer

import org.junit.Test

class TemporaryVariableEliminationTest : BasicOptimizerTest("temporary-variable") {
    @Test fun declaration() = box()

    @Test fun assignment() = box()

    @Test fun skipsGlobalDeclarations() = box()

    @Test fun methodCall() = box()

    @Test fun ifBranch() = box()

    @Test fun tryCatch() = box()

    @Test fun nonSideEffect() = box()

    @Test fun propertyAccess() = box()

    @Test fun removeUnused() = box()

    @Test fun innerExpressionProcessed() = box()

    @Test fun transitiveNotConsideredTrivial() = box()

    @Test fun assignmentToNonLocal() = box()

    @Test fun removeUnusedAndSubstitute() = box()

    @Test fun assignmentToOuterVar() = box()

    @Test fun shortCircuit() = box()
}