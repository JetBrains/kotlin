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

import com.google.common.collect.Lists
import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.js.facade.MainCallParameters
import org.jetbrains.kotlin.js.test.BasicBoxTest
import org.jetbrains.kotlin.js.test.NashornJsTestChecker
import java.io.File
import javax.script.ScriptException

abstract class AbstractWebDemoExamplesTest(relativePath: String) : BasicBoxTest(
        BasicBoxTest.TEST_DATA_DIR_PATH + "/$relativePath/",
        relativePath,
        generateNodeJsRunner = false
) {
    override fun runGeneratedCode(
            jsFiles: List<String>,
            testModuleName: String,
            testPackage: String?,
            testFunction: String,
            expectedResult: String,
            withModuleSystem: Boolean
    ) {
        NashornJsTestChecker.checkStdout(jsFiles, expectedResult)
    }

    @Throws(ScriptException::class)
    protected fun runMainAndCheckOutput(fileName: String, expectedResult: String, vararg args: String) {
        doTest(pathToTestDir + fileName, expectedResult, MainCallParameters.mainWithArguments(Lists.newArrayList(*args)))
    }

    protected fun runMainAndCheckOutputWithExpectedFile(testName: String, testId: String, vararg args: String) {
        val expectedResult = StringUtil.convertLineSeparators(File(pathToTestDir + testName + testId + ".out").readText())
        doTest(pathToTestDir + testName + ".kt", expectedResult, MainCallParameters.mainWithArguments(Lists.newArrayList(*args)))
    }
}