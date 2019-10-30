/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.uast.test.kotlin.analysis

import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.UFile
import org.jetbrains.uast.UastLanguagePlugin
import org.jetbrains.uast.analysis.UExpressionFact
import org.jetbrains.uast.analysis.UNullability
import org.jetbrains.uast.getContainingUMethod
import org.jetbrains.uast.kotlin.analysis.KotlinUastAnalysisPlugin
import org.jetbrains.uast.test.kotlin.AbstractKotlinUastTest
import org.jetbrains.uast.visitor.AbstractUastVisitor

@Suppress("UnstableApiUsage")
class KotlinUastAnalysisPluginTest : AbstractKotlinUastTest() {
    override fun check(testName: String, file: UFile) {
        val uastAnalysisPlugin = UastLanguagePlugin.byLanguage(KotlinLanguage.INSTANCE)?.analysisPlugin
            ?: kotlin.test.fail("cannot get plugin")
        assertTrue(uastAnalysisPlugin is KotlinUastAnalysisPlugin)

        val expected = mutableMapOf<String, UNullability>()
        val actual = mutableMapOf<String, UNullability?>()

        file.accept(object : AbstractUastVisitor() {
            override fun visitExpression(node: UExpression): Boolean {
                val nullability = node.comments.firstOrNull()?.text?.removePrefix("/*")?.removeSuffix("*/")?.let {
                    UNullability.valueOf(it)
                } ?: return super.visitExpression(node)

                with(uastAnalysisPlugin) {
                    val message = "in method `${node.getContainingUMethod()?.name}` expr: `${node.asRenderString()}`"
                    expected[message] = nullability
                    actual[message] = node.getExpressionFact(UExpressionFact.UNullabilityFact)
                }
                return super.visitExpression(node)
            }
        })

        assertEquals(expected, actual)
    }


    fun testNullabilityAnalysis() = doTest("NullabilityAnalysis")
}