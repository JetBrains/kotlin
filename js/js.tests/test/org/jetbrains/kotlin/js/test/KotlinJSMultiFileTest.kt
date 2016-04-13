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

package org.jetbrains.kotlin.js.test

import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.KotlinTestUtils.TestFileFactory
import java.io.File

abstract class KotlinJSMultiFileTest(path: String) : SingleFileTranslationTest(path) {
    var tempDir: File? = null

    override fun doTest(filename: String) {
        tempDir = FileUtil.createTempDirectory("test", "src")
        tempDir!!.deleteOnExit()
        try {
            val file = File(filename)
            val expectedText = KotlinTestUtils.doLoadFile(file)
            var testPackage: String? = null

            val inputFiles = KotlinTestUtils.createTestFiles(file.name, expectedText, object : TestFileFactory<String, String> {
                override fun createFile(module: String?, fileName: String, text: String, directives: Map<String, String>): String? {
                    val output = File(tempDir!!, fileName)
                    KotlinTestUtils.mkdirs(file.parentFile)
                    output.writeText(text, Charsets.UTF_8)

                    val ktFile = KtPsiFactory(project).createFile(text)
                    val boxFunction = ktFile.declarations.find { it is KtNamedFunction && it.name == TEST_FUNCTION }
                    if (boxFunction != null) {
                        testPackage = getPackageName(ktFile)
                    }

                    return output.path
                }

                override fun createModule(name: String, dependencies: List<String>): String? = error("Multi-module tests not supported")
            })
            runFunctionOutputTestByPaths(BasicTest.DEFAULT_ECMA_VERSIONS, inputFiles, testPackage!!, BasicTest.TEST_FUNCTION, "OK")
        }
        finally {
            tempDir!!.deleteRecursively()
        }
    }
}