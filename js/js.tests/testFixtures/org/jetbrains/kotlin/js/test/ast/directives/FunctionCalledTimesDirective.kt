/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.ast.directives

import org.jetbrains.kotlin.js.backend.ast.JsNode
import org.jetbrains.kotlin.js.testOld.utils.ArgumentsHelper
import org.jetbrains.kotlin.js.testOld.utils.CallCounter
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertEquals
import java.io.File

class FunctionCalledTimesDirective(entry: String) : ArgumentsHelper(entry), JsAstDirective {
    val count by requiredInt()
    val functionName by positional(0)

    override fun evaluate(ast: JsNode, sourceFile: File) {
        val counter = CallCounter.countCalls(ast)
        val actualCount = counter.getUnqualifiedCallsCount(functionName)
        assertEquals(count, actualCount) { "Unexpected call count for function $functionName" }
    }
}
