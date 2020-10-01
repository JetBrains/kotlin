/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion.test.handlers

import org.jetbrains.kotlin.idea.completion.FIR_COMPARISON
import org.jetbrains.kotlin.idea.completion.runTestWithCustomEnableDirective

abstract class AbstractHighLevelBasicCompletionHandlerTest : AbstractBasicCompletionHandlerTest() {
    override val captureExceptions: Boolean = false

    override fun doTest(testPath: String) {
        runTestWithCustomEnableDirective(FIR_COMPARISON, testDataFile()) { super.doTest(testPath) }
    }
}