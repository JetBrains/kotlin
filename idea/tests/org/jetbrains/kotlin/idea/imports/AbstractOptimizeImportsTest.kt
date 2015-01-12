/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.imports

import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.command.UndoConfirmationPolicy
import org.jetbrains.kotlin.idea.JetLightCodeInsightFixtureTestCase
import java.io.File
import org.junit.Assert
import org.jetbrains.kotlin.idea.PluginTestCaseBase

public abstract class AbstractOptimizeImportsTest() : JetLightCodeInsightFixtureTestCase() {

    public fun doTest(path: String) {
        val fixture = myFixture!!
        fixture.setTestDataPath(path)
        val (expectedFile, testFiles) = findTestFiles(path)
        fixture.configureByFiles(*testFiles.map { it.name : String? }.copyToArray())
        CommandProcessor.getInstance()!!.executeCommand(
                getProject(),
                KotlinImportOptimizer().processFile(fixture.getFile()),
                "Optimize Imports",
                null,
                UndoConfirmationPolicy.DO_NOT_REQUEST_CONFIRMATION
        )
        fixture.checkResultByFile(expectedFile.name, true)
    }

    private fun findTestFiles(path: String): Pair<File, List<File>> {
        val files = File(path).listFiles()!!
        val testName = getTestName(false)

        val expectedFileName = "$testName.after.kt"
        val expectedFile = files.find { it.name == expectedFileName }
        Assert.assertNotNull("Can't find $expectedFileName", expectedFile)

        val fileToBeOptimizedName = "$testName.kt"
        val fileToBeOptimized = files.find { it.name == fileToBeOptimizedName }
        Assert.assertNotNull("Can't find $fileToBeOptimizedName", fileToBeOptimized)

        val testFiles = listOf(fileToBeOptimized!!) + files.filter { it != fileToBeOptimized && it != expectedFile }
        return Pair(expectedFile!!, testFiles)
    }

    override fun getTestDataPath() = "${PluginTestCaseBase.getTestDataPathBase()}/editor/optimizeImports/${getTestName(false)}"
}
