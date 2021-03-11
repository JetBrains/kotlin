/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.perf

import com.intellij.codeInsight.completion.CompletionType
import org.jetbrains.kotlin.test.uitls.IgnoreTests.DIRECTIVES
import org.jetbrains.kotlin.test.InTextDirectivesUtils

abstract class AbstractHighLevelPerformanceCompletionHandlerTests(
    defaultCompletionType: CompletionType,
    note: String = ""
) : AbstractPerformanceCompletionHandlerTests(defaultCompletionType, note) {

    override val statsPrefix: String = "fir-completion"

    override fun doPerfTest(unused: String) {
        if (!InTextDirectivesUtils.isDirectiveDefined(testDataFile().readText(), DIRECTIVES.FIR_COMPARISON)) return

        super.doPerfTest(unused)
    }
}

abstract class AbstractHighLevelPerformanceBasicCompletionHandlerTest :
    AbstractHighLevelPerformanceCompletionHandlerTests(CompletionType.BASIC)
