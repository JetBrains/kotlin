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

package org.jetbrains.kotlin.js.test.semantics

import org.jetbrains.kotlin.js.test.BasicBoxTest
import java.io.File

abstract class AbstractOutputPrefixPostfixTest : BasicBoxTest(
        BasicBoxTest.TEST_DATA_DIR_PATH + "outputPrefixPostfix/",
        "${BasicBoxTest.TEST_DATA_DIR_PATH}/out/outputPrefixPostfix/",
        generateNodeJsRunner = false
) {
    override fun getOutputPrefixFile(testFilePath: String): File? {
        return newFileIfExists(testFilePath + ".prefix")
    }

    override fun getOutputPostfixFile(testFilePath: String): File? {
        return newFileIfExists(testFilePath + ".postfix")
    }

    override fun performAdditionalChecks(generatedJsFiles: List<String>, outputPrefixFile: File?, outputPostfixFile: File?) {
        super.performAdditionalChecks(generatedJsFiles, outputPrefixFile, outputPostfixFile)

        val output = File(generatedJsFiles.first()).readText()

        outputPrefixFile?.let {
            assertTrue(output.startsWith(it.readText()))
        }
        outputPostfixFile?.let {
            assertTrue(output.endsWith(it.readText()))
        }
    }

    private fun newFileIfExists(path: String): File? {
        val file = File(path)
        if (!file.exists()) return null
        return file
    }
}