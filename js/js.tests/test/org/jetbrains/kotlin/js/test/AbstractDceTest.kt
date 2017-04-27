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

package org.jetbrains.kotlin.js.test

import junit.framework.TestCase
import org.jetbrains.kotlin.js.dce.DeadCodeElimination
import org.jetbrains.kotlin.js.dce.InputFile
import java.io.File

abstract class AbstractDceTest : TestCase() {
    fun doTest(filePath: String) {
        val file = File(filePath)
        val fileContents = file.readText()
        val inputFile = InputFile(filePath, File(pathToOutputDir, file.relativeTo(File(pathToTestDir)).path).path, "main")
        val dceResult = DeadCodeElimination.run(setOf(inputFile), extractDeclarations(REQUEST_REACHABLE_PATTERN, fileContents)) {}
        val reachableNodeStrings = dceResult.reachableNodes.map { it.toString().removePrefix("<unknown>.") }.toSet()

        for (assertedDeclaration in extractDeclarations(ASSERT_REACHABLE_PATTERN, fileContents)) {
            TestCase.assertTrue("Declaration $assertedDeclaration not reached", assertedDeclaration in reachableNodeStrings)
        }
        for (assertedDeclaration in extractDeclarations(ASSERT_UNREACHABLE_PATTERN, fileContents)) {
            TestCase.assertTrue("Declaration $assertedDeclaration reached", assertedDeclaration !in reachableNodeStrings)
        }
    }

    private fun extractDeclarations(regex: Regex, fileContents: String): Set<String> =
            regex.findAll(fileContents).map { it.groupValues[1] }.toSet()

    companion object {
        private val ASSERT_REACHABLE_PATTERN = Regex("^ *// *ASSERT_REACHABLE: (.+) *$", RegexOption.MULTILINE)
        private val ASSERT_UNREACHABLE_PATTERN = Regex("^ *// *ASSERT_UNREACHABLE: (.+) *$", RegexOption.MULTILINE)
        private val REQUEST_REACHABLE_PATTERN = Regex("^ *// *REQUEST_REACHABLE: (.+) *$", RegexOption.MULTILINE)

        private val pathToTestDir = "js/js.translator/testData/dce"
        private val pathToOutputDir = "js/js.translator/testData/out/dce"
    }
}