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

import org.jetbrains.kotlin.js.test.BasicBoxTest
import org.jetbrains.kotlin.js.test.NashornJsTestChecker
import java.io.File
import javax.script.ScriptException

class MultiModuleOrderTest : BasicBoxTest("$TEST_DATA_DIR_PATH/multiModuleOrder/cases/", "$TEST_DATA_DIR_PATH/multiModuleOrder/out/") {
    fun testPlain() {
        runTest("plain")
    }

    fun testUmd() {
        runTest("umd")
    }

    fun runTest(name: String) {
        val fullPath = "$TEST_DATA_DIR_PATH/multiModuleOrder/cases/$name.kt"
        doTest(fullPath)
        checkWrongOrderReported(fullPath, name)
    }

    private fun checkWrongOrderReported(path: String, name: String) {
        val parentDir = getOutputDir(File(path))
        val mainJsFile = File(parentDir, "$name-main_v5.js").path
        val libJsFile = File(parentDir, "$name-lib_v5.js").path
        try {
            NashornJsTestChecker.run(listOf(mainJsFile, libJsFile))
        }
        catch (e: ScriptException) {
            val message = e.message!!
            assertTrue("Exception message should contain reference to dependency (lib)", "'lib'" in message)
            assertTrue("Exception message should contain reference to module that failed to load (main)", "'main'" in message)
            return
        }
        fail("Exception should have been thrown due to wrong order of modules")
    }
}