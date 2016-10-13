/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.uast

import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor

abstract class AbstractKotlinUastStructureTest : KotlinLightCodeInsightFixtureTestCase() {
    fun doTest() {
//        val testName = getTestName(false)
//        myFixture.configureByFile("$testName.kt")
//
//        val logFile = File(File(testDataPath, "log"), "$testName.txt")
//        val renderFile = File(File(testDataPath, "render"), "$testName.txt")
//        val treeFile = File(File(testDataPath, "tree"), "$testName.txt")
//
//        val psiFile = myFixture.file
//        val uElement = KotlinUastLanguagePlugin.converter.convertWithParent(psiFile) ?: error("UFile was not created")
//
//        val logActual = uElement.logString()
//        val renderActual = trimEmptyLines(uElement.renderString())
//
//        try {
//            KotlinTestUtils.assertEqualsToFile(logFile, logActual)
//        } catch (e: Throwable) {
//            KotlinTestUtils.assertEqualsToFile(renderFile, renderActual)
//            throw e
//        }
//        KotlinTestUtils.assertEqualsToFile(renderFile, renderActual)
//        KotlinTestUtils.assertEqualsToFile(treeFile, genTree(uElement))
    }

//    private fun trimEmptyLines(s: String): String {
//        if (true) return s
//        val lineSeparator = System.getProperty("line.separator")
//        return s.lines().map { if (it.trim().isEmpty()) "" else it.trimEnd() }.joinToString(lineSeparator)
//    }

//    private fun genTree(node: UElement): String {
//        val builder = StringBuilder()
//        val visitor = object : AbstractUastVisitor() {
//            private tailrec fun height(node: UElement, current: Int): Int {
//                val parent = node.parent ?: return current
//                return height(parent, current + 1)
//            }
//
//            override fun visitElement(node: UElement): Boolean {
//                builder.appendln("    ".repeat(height(node, 0)) + node.javaClass.simpleName)
//                return super.visitElement(node)
//            }
//        }
//        node.accept(visitor)
//        return builder.toString()
//    }

    override fun getTestDataPath() = "plugins/uast-kotlin/testData"

    override fun getProjectDescriptor() = KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE
}