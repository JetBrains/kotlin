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

package org.jetbrains.kotlin.annotation.processing.test.processor

import java.io.File
import javax.lang.model.element.TypeElement
import javax.tools.JavaFileObject

class TestsWithFiler : AbstractProcessorTest() {
    override val testDataDir = "plugins/annotation-processing/testData/withFiler"
    
    fun testSimple() = filerTest("Simple", 1, "generated/Inject2Test.java")
    
    fun testMethodsFields() = filerTest("MethodsFields", 1,
                                        "generated/InjectmyField.java",
                                        "generated/InjectmyFunc.java")
    
    fun testTwoRounds() = filerTest("TwoRounds", 2,
                                    "generated/Inject2InjectTest.java",
                                    "generated/InjectTest.java",
                                    "generated/InjectmyFunc.java")
    
    fun testOneRound() = filerTest("OneRound", 1,
                                   "generated/Inject2Test.java",
                                   "generated/Inject2myFunc.java",
                                   "generated/InjectmyFunc.java")
    
    fun testZeroRounds() = filerTest("ZeroRounds", 0)
    
    private fun filerTest(
            fileName: String,
            roundCount: Int,
            vararg expectedFiles: String
    ) {
        val filesCreated = mutableSetOf<JavaFileObject>()
        var actualRoundCount = 0

        testAP(roundCount > 0, fileName, { set, roundEnv, env ->
            if (!roundEnv.processingOver()) actualRoundCount++

            for (anno in set) {
                val annotated = roundEnv.getElementsAnnotatedWith(anno)
                annotated.forEach { el ->
                    val className = anno.simpleName.toString() + el.simpleName.toString()
                    env.filer.createSourceFile("generated.$className").apply {
                        filesCreated += this
                        val inject2 = if (el is TypeElement && anno.simpleName.toString() == "Inject") "@Inject2\n" else ""
                        openWriter().use {
                            it.write("package generated;\n" + inject2 +
                                     "public class $className {}")
                        }
                    }
                }
            }
        }, "Inject", "Inject2")

        assertEquals(roundCount, actualRoundCount)
        assertEquals(expectedFiles.map { it.replace('/', File.separatorChar) }, filesCreated.map { it.name }.sorted())
    }
}