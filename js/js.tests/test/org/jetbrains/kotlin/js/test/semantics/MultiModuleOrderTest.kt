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

package org.jetbrains.kotlin.js.test.semantics

import com.eclipsesource.v8.V8ScriptException
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
            assertTrue(e is ScriptException || e is V8ScriptException)
            val message = e.message!!
            assertTrue("Exception message should contain reference to dependency (lib)", "'lib'" in message)
            assertTrue("Exception message should contain reference to module that failed to load (main)", "'main'" in message)
            return
        }
        fail("Exception should have been thrown due to wrong order of modules")
    }
}