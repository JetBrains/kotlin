/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package org.jetbrains.uast

import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.testFramework.LightCodeInsightTestCase
import com.intellij.testFramework.LightPlatformTestCase
import java.io.File

open class AbstractStructureTest : LightCodeInsightTestCase() {
    fun test(name: String) {
//        val testDir = testDataPath
//        val javaFile = File(testDir, "$name.java")
//        val logFile = File(File(testDir, "log"), "$name.txt")
//        val renderFile = File(File(testDir, "render"), "$name.txt")
//
//        val psiClass = createClass(javaFile.readText())
//        val uElement = JavaConverter.convertWithParent(psiClass) ?: error("UFile was not created")
//        val uFile = uElement.getContainingFile() ?: error("No containing file")
//        try {
//            assertEqualsFile(uFile.logString(), logFile)
//        } catch (e: NoTestFileException) {
//            assertEqualsFile(uFile.renderString(), renderFile)
//            throw e
//        }
//        assertEqualsFile(uFile.renderString(), renderFile)
    }

    private fun createClass(text: String): PsiClass {
        val factory = JavaPsiFacade.getInstance(LightPlatformTestCase.ourProject).elementFactory
        val classA = factory.createClassFromText(text, null).innerClasses[0]
        return classA
    }

    private fun assertEqualsFile(text: String, file: File) {
        if (!file.exists()) {
            file.parentFile.mkdirs()
            file.writeText(text)
            throw NoTestFileException(file)
        } else {
            val lineSeparator = System.getProperty("line.separator") ?: "\n";
            val expected = file.readLines().map { it.trimEnd() }.joinToString(lineSeparator).trim()
            val actual = text.lines().map { it.trimEnd() }.joinToString(lineSeparator).trim()
            assertEquals(expected, actual)
        }
    }

    private class NoTestFileException(file: File) : RuntimeException("Test file was generated: $file")

    override fun getTestDataPath() = "plugins/uast-java/testData"
}