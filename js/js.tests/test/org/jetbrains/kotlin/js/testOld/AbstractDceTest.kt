/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.testOld

import junit.framework.TestCase
import org.jetbrains.kotlin.js.dce.DeadCodeElimination
import org.jetbrains.kotlin.js.dce.InputFile
import org.jetbrains.kotlin.js.dce.InputResource
import java.io.File

abstract class AbstractDceTest : TestCase() {
    fun doTest(filePath: String) {
        val file = File(filePath)
        val fileContents = file.readText()
        val inputFile = InputFile(InputResource.file(filePath), null,
                                  File(pathToOutputDir, file.relativeTo(File(pathToTestDir)).path).path, "main")
        val dceResult = DeadCodeElimination.run(setOf(inputFile), extractDeclarations(REQUEST_REACHABLE_PATTERN, fileContents), true) { _, _ -> }
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