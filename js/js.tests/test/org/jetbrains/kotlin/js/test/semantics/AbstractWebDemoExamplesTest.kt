/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.semantics

import com.eclipsesource.v8.V8ScriptException
import com.google.common.collect.Lists
import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.js.facade.MainCallParameters
import org.jetbrains.kotlin.js.test.*
import java.io.File
import javax.script.ScriptException

abstract class AbstractWebDemoExamplesTest(relativePath: String) : BasicBoxTest(
        BasicBoxTest.TEST_DATA_DIR_PATH + "/$relativePath/",
        relativePath,
        generateNodeJsRunner = false
) {
    override fun runGeneratedCode(
        jsFiles: List<String>,
        testModuleName: String?,
        testPackage: String?,
        testFunction: String,
        expectedResult: String,
        withModuleSystem: Boolean
    ) {
        testChecker.checkStdout(jsFiles, expectedResult)
    }

    @Throws(ScriptException::class, V8ScriptException::class)
    protected fun runMainAndCheckOutput(fileName: String, expectedResult: String, vararg args: String) {
        doTest(pathToTestDir + fileName, expectedResult, MainCallParameters.mainWithArguments(Lists.newArrayList(*args)))
    }

    protected fun runMainAndCheckOutputWithExpectedFile(testName: String, testId: String, vararg args: String) {
        val expectedResult = StringUtil.convertLineSeparators(File(pathToTestDir + testName + testId + ".out").readText())
        doTest(pathToTestDir + testName + ".kt", expectedResult, MainCallParameters.mainWithArguments(Lists.newArrayList(*args)))
    }
}