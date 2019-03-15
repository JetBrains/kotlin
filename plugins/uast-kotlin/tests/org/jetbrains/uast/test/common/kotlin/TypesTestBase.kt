/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.uast.test.common.kotlin

import org.jetbrains.uast.UElement
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.UFile
import org.jetbrains.uast.test.env.kotlin.assertEqualsToFile
import org.jetbrains.uast.visitor.UastVisitor
import java.io.File

interface TypesTestBase {
    fun getTypesFile(testName: String): File

    private fun UFile.asLogTypes() = TypesLogger().apply {
        this@asLogTypes.accept(this)
    }.toString()

    fun check(testName: String, file: UFile) {
        val valuesFile = getTypesFile(testName)

        assertEqualsToFile("Log values", valuesFile, file.asLogTypes())
    }

    class TypesLogger : UastVisitor {

        val builder = StringBuilder()

        var level = 0

        override fun visitElement(node: UElement): Boolean {
            val initialLine = node.asLogString() + " [" + run {
                val renderString = node.asRenderString().lines()
                if (renderString.size == 1) {
                    renderString.single()
                } else {
                    renderString.first() + "..." + renderString.last()
                }
            } + "]"

            (1..level).forEach { builder.append("    ") }
            builder.append(initialLine)
            if (node is UExpression) {
                val value = node.getExpressionType()
                value?.let { builder.append(" : ").append(it) }
            }
            builder.appendln()
            level++
            return false
        }

        override fun afterVisitElement(node: UElement) {
            level--
        }

        override fun toString() = builder.toString()
    }
}