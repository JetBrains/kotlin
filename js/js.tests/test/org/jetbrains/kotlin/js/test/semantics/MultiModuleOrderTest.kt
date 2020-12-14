/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.semantics

import org.jetbrains.kotlin.js.test.BasicBoxTest
import java.io.File
import javax.script.ScriptException

private val testGroupDir = "multiModuleOrder/"
private val pathToTestGroupDir = BasicBoxTest.TEST_DATA_DIR_PATH + testGroupDir

class MultiModuleOrderTest : BasicBoxTest(pathToTestGroupDir, testGroupDir) {
    fun testPlain() {
        runTest("plain")
    }

    fun testUmd() {
        runTest("umd")
    }

    fun runTest(name: String) {
        val fullPath = pathToTestGroupDir + "$name.kt"
        doTest(fullPath)
        checkWrongOrderReported(fullPath, name)
    }

    private fun checkWrongOrderReported(path: String, name: String) {
        val parentDir = getOutputDir(File(path))
        val mainJsFile = File(parentDir, "$name-main_v5.js").path
        val libJsFile = File(parentDir, "$name-lib_v5.js").path
        try {
            testChecker.run(listOf(mainJsFile, libJsFile))
        }
        catch (e: RuntimeException) {
            assertTrue(e is ScriptException || e is IllegalStateException)
            val message = e.message!!
            assertTrue("Exception message should contain reference to dependency (lib)", "'lib'" in message)
            assertTrue("Exception message should contain reference to module that failed to load (main)", "'main'" in message)
            return
        }
        fail("Exception should have been thrown due to wrong order of modules")
    }
}