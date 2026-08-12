/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.ast.directives

import org.jetbrains.kotlin.js.backend.ast.JsNode
import org.jetbrains.kotlin.js.testOld.utils.ArgumentsHelper
import org.jetbrains.kotlin.js.testOld.utils.AstSearchUtil
import org.jetbrains.kotlin.js.testOld.utils.CallCounter
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertEquals
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertNotEquals
import java.io.File

class HasCallsDirective(entry: String, private val inverted: Boolean = false) : ArgumentsHelper(entry), JsAstDirective {
    val function by required()
    val scope by required()
    val qualified by boolean()

    override fun evaluate(ast: JsNode, sourceFile: File) {
        val scopeFunction = AstSearchUtil.getFunction(ast, scope)
        val counter = CallCounter.countCalls(scopeFunction)
        val callCount = if (qualified) {
            counter.getQualifiedCallsCount(function)
        } else {
            counter.getUnqualifiedCallsCount(function)
        }
        if (inverted) {
            assertEquals(0, callCount) { "$function is called inside $scope" }
        } else {
            assertNotEquals(0, callCount) { "$function is not called inside $scope" }
        }
    }
}
