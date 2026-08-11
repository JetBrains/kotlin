/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.ast.directives

import org.jetbrains.kotlin.js.backend.ast.JsNode
import org.jetbrains.kotlin.js.testOld.utils.ArgumentsHelper
import org.jetbrains.kotlin.js.testOld.utils.DirectiveTestUtils
import org.jetbrains.kotlin.js.testOld.utils.PropertyReferenceCollector.Companion.collect
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertEquals
import java.io.File

class PropertyCountingDirective(
    entry: String,
    private val expectedReadCount: Int,
    private val expectedWriteCount: Int,
) : ArgumentsHelper(entry), JsAstDirective {

    companion object {
        const val ANY_COUNT: Int = -1
        const val FROM_ARGUMENT: Int = -2
    }

    val propertyName by positional(0)
    val scope by optional()
    val count by requiredInt()

    private fun getCount(expected: Int): Int = if (expected == FROM_ARGUMENT) count else expected

    override fun evaluate(ast: JsNode, sourceFile: File) {
        val counter = collect(DirectiveTestUtils.findScope(ast, scope))

        val expectedReadCount = getCount(this.expectedReadCount)
        if (expectedReadCount != ANY_COUNT) {
            assertEquals(expectedReadCount, counter.unqualifiedReadCount(propertyName)) {
                "Unexpected read count for property $propertyName in scope $scope"
            }
        }

        val expectedWriteCount = getCount(this.expectedWriteCount)
        if (expectedWriteCount != ANY_COUNT) {
            assertEquals(expectedWriteCount, counter.unqualifiedWriteCount(propertyName)) {
                "Unexpected write count for property $propertyName in scope $scope"
            }
        }
    }
}
