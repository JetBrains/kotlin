/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.testOld.optimizer

import org.junit.Test

class WhileConditionFoldingTest : BasicOptimizerTest("while-condition-folding") {
    @Test fun simpleWhile() = box()

    @Test fun simpleDoWhile() = box()

    @Test fun consequentConditions() = box()

    @Test fun nestedConditions() = box()

    @Test fun doWhileWithContinue() = box()

    @Test fun whileEvaluationOrder() = box()

    @Test fun doWhileEvaluationOrder() = box()

    @Test fun inNestedLoop() = box()

    @Test fun inLabeledBlock() = box()

    @Test fun doWhileWithNestedContinue() = box()

    @Test fun labeledContinueInNestedLoop() = box()

    @Test fun labeledBreak() = box()
}