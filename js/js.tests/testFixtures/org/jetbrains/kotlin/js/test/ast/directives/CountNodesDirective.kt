/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.js.test.ast.directives

import org.jetbrains.kotlin.js.backend.ast.HasName
import org.jetbrains.kotlin.js.backend.ast.JsNode
import org.jetbrains.kotlin.js.inline.util.collectInstances
import org.jetbrains.kotlin.js.testOld.utils.ArgumentsHelper
import org.jetbrains.kotlin.js.testOld.utils.AstSearchUtil.getFunction
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertEquals
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertTrue
import java.io.File
import kotlin.reflect.KClass

class CountNodesDirective<T : JsNode>(entry: String, private val klass: KClass<T>) : ArgumentsHelper(entry), JsAstDirective {
    companion object {
        inline fun <reified T : JsNode> counting(): (String) -> CountNodesDirective<T> = { CountNodesDirective(it, T::class) }
    }

    val function by required()
    val count by optionalInt()
    val max by optionalInt()
    val includeNestedDeclarations by boolean()
    val name by optional()

    override fun evaluate(ast: JsNode, sourceFile: File) {
        val functionName = function
        val count = count
        val maxCount = max

        val function = getFunction(ast, functionName)
        val nodes = collectInstances(klass, function.body, includeNestedDeclarations)
        val actualCount = nodes.fold(0) { acc, node -> acc + getActualCountFor(node) }

        if (count != null) {
            assertEquals(count, actualCount) {
                "Function $functionName contains $actualCount nodes of type ${klass.simpleName}, but expected count is $count"
            }
        } else if (maxCount != null) {
            assertTrue(maxCount >= actualCount) {
                "Function $functionName contains $actualCount nodes of type ${klass.simpleName}, but expected max is $maxCount"
            }
        } else {
            throw IllegalArgumentException("'max' or 'count' argument should be provided")
        }
    }

    private fun getActualCountFor(node: T): Int {
        if (node is HasName) {
            name?.let {
                return if (node.name?.ident == it) 1 else 0
            }
        }
        return 1
    }
}
