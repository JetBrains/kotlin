/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.uast.test.common

import org.jetbrains.uast.UElement
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.UFile
import org.jetbrains.uast.evaluation.UEvaluationContext
import org.jetbrains.uast.evaluation.UEvaluatorExtension
import org.jetbrains.uast.evaluation.analyzeAll
import org.jetbrains.uast.test.env.assertEqualsToFile
import org.jetbrains.uast.visitor.UastVisitor
import java.io.File

interface ValuesTestBase {
    fun getValuesFile(testName: String): File
    fun getEvaluatorExtension(): UEvaluatorExtension? = null

    private fun UFile.asLogValues(): String {
        val evaluationContext = analyzeAll(extensions = getEvaluatorExtension()?.let { listOf(it) } ?: emptyList())
        return ValueLogger(evaluationContext).apply {
            this@asLogValues.accept(this)
        }.toString()
    }

    fun check(testName: String, file: UFile) {
        val valuesFile = getValuesFile(testName)

        assertEqualsToFile("Log values", valuesFile, file.asLogValues())
    }

    class ValueLogger(val evaluationContext: UEvaluationContext) : UastVisitor {

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
                val value = evaluationContext.valueOf(node)
                builder.append(" = ").append(value)
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