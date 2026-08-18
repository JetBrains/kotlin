/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.ast.directives

import org.jetbrains.kotlin.js.backend.ast.JsNode
import org.jetbrains.kotlin.js.testOld.utils.ArgumentsHelper
import org.jetbrains.kotlin.js.testOld.utils.AstSearchUtil.getFunction
import org.jetbrains.kotlin.js.testOld.utils.CallCounter
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertEquals
import java.io.File

class CheckContainsNoCallsDirective(entry: String) : ArgumentsHelper(entry), JsAstDirective {
    val functionName by positional(0)
    val except by set()

    override fun evaluate(ast: JsNode, sourceFile: File) {
        val function = getFunction(ast, functionName)
        val counter = CallCounter.countCalls(function, except)
        assertEquals(0, counter.totalCallsCount) { "$functionName contains calls" }
    }
}
