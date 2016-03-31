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
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.uast.UElement
import org.jetbrains.uast.visitor.UastVisitor
import java.io.File

abstract class AbstractKotlinUastStructureTest : KotlinLightCodeInsightFixtureTestCase() {
    fun doTest() {
        val testName = getTestName(false)
        myFixture.configureByFile("$testName.kt")

        val logFile = File(File(testDataPath, "log"), "$testName.txt")
        val renderFile = File(File(testDataPath, "render"), "$testName.txt")
        val treeFile = File(File(testDataPath, "tree"), "$testName.txt")

        val psiFile = myFixture.file
        val uElement = KotlinUastLanguagePlugin.converter.convertWithParent(psiFile) ?: error("UFile was not created")

        val logActual = uElement.logString()
        val renderActual = uElement.renderString()

        try {
            KotlinTestUtils.assertEqualsToFile(logFile, logActual)
        } catch (e: Throwable) {
            KotlinTestUtils.assertEqualsToFile(renderFile, renderActual)
            throw e
        }
        KotlinTestUtils.assertEqualsToFile(renderFile, renderActual)
        KotlinTestUtils.assertEqualsToFile(treeFile, genTree(uElement))
    }

    private fun genTree(node: UElement): String {
        val builder = StringBuilder()
        val visitor = object : UastVisitor() {
            private tailrec fun getParentCount(node: UElement): Int {
                val parent = node.parent ?: return 0
                return getParentCount(parent)
            }

            override fun visitElement(node: UElement): Boolean {
                builder.appendln("    ".repeat(getParentCount(node)) + node.javaClass.name)
                return super.visitElement(node)
            }
        }
        visitor.visitElement(node)
        return builder.toString()
    }

    override fun getTestDataPath() = "plugins/uast-kotlin/testData"

    override fun getProjectDescriptor() = KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE
}