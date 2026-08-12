/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.js.test.ast.directives

import org.jetbrains.kotlin.js.backend.ast.JsNode
import org.jetbrains.kotlin.js.inline.util.collectInstances
import org.jetbrains.kotlin.js.testOld.utils.ArgumentsHelper
import org.jetbrains.kotlin.js.testOld.utils.AstSearchUtil.getFunction
import org.jetbrains.kotlin.js.testOld.utils.DirectiveTestUtils
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertEquals
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertTrue
import java.io.File

open class CountNodesDirective<T : JsNode>(private val klass: Class<T>) : DirectiveTestUtils.DirectiveHandler<ArgumentsHelper>() {
    override fun processEntry(ast: JsNode, arguments: ArgumentsHelper, sourceFile: File?) {
        val functionName = arguments.getNamedArgument("function")
        val countStr = arguments.findNamedArgument("count")
        val maxCountStr = arguments.findNamedArgument("max")
        val includeNestedDeclarations = arguments.findNamedArgument("includeNestedDeclarations")

        val function = getFunction(ast, functionName)
        val nodes = collectInstances(klass, function.body, includeNestedDeclarations != null && includeNestedDeclarations == "true")
        val actualCount = nodes.fold(0) { acc, node -> acc + getActualCountFor(node, arguments) }

        if (countStr != null) {
            val expectedCount = countStr.toInt()
            assertEquals(expectedCount, actualCount) {
                "Function $functionName contains $actualCount nodes of type ${klass.getName()}, but expected count is $expectedCount"
            }
        } else if (maxCountStr != null) {
            val expectedCount = maxCountStr.toInt()
            assertTrue(expectedCount >= actualCount) {
                "Function $functionName contains $actualCount nodes of type ${klass.getName()}, but expected max is $expectedCount"
            }
        } else {
            throw IllegalArgumentException("'max' or 'count' argument should be provided")
        }
    }

    protected open fun getActualCountFor(node: T, arguments: ArgumentsHelper): Int = 1
}
