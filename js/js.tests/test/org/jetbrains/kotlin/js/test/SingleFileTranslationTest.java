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

package org.jetbrains.kotlin.js.test;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.js.config.EcmaVersion;
import org.jetbrains.kotlin.js.facade.MainCallParameters;
import org.jetbrains.kotlin.js.test.rhino.RhinoFunctionResultChecker;
import org.jetbrains.kotlin.js.test.rhino.RhinoSystemOutputChecker;

import java.util.Collections;
import java.util.List;

import static org.jetbrains.kotlin.js.test.utils.JsTestUtils.readFile;

@SuppressWarnings("JUnitTestCaseWithNonTrivialConstructors")
public abstract class SingleFileTranslationTest extends BasicTest {
    public SingleFileTranslationTest(@NotNull String main) {
        super(main);
    }

    protected void runFunctionOutputTest(
            @NotNull String kotlinFilename,
            @NotNull String packageName,
            @NotNull String functionName,
            @NotNull Object expectedResult
    ) throws Exception {
        runFunctionOutputTest(DEFAULT_ECMA_VERSIONS, kotlinFilename, packageName, functionName, expectedResult);
    }

    protected void runFunctionOutputTest(
            @NotNull Iterable<EcmaVersion> ecmaVersions,
            @NotNull String kotlinFilename,
            @NotNull String packageName,
            @NotNull String functionName,
            @NotNull Object expectedResult
    ) throws Exception {
        runFunctionOutputTestByPath(ecmaVersions, getInputFilePath(kotlinFilename), packageName, functionName, expectedResult);
    }

    private void runFunctionOutputTestByPath(
            @NotNull Iterable<EcmaVersion> ecmaVersions,
            @NotNull String kotlinFilePath,
            @NotNull String packageName,
            @NotNull String functionName,
            @NotNull Object expectedResult
    ) throws Exception {
        runFunctionOutputTestByPaths(ecmaVersions, Collections.singletonList(kotlinFilePath), packageName, functionName, expectedResult);
    }

    private void runFunctionOutputTestByPaths(
            @NotNull Iterable<EcmaVersion> ecmaVersions,
            @NotNull List<String> kotlinFilePaths,
            @NotNull String packageName,
            @NotNull String functionName,
            @NotNull Object expectedResult
    ) throws Exception {
        String testName = getTestName(true);
        generateJavaScriptFiles(kotlinFilePaths, testName, MainCallParameters.noCall(), ecmaVersions);
        RhinoFunctionResultChecker checker = new RhinoFunctionResultChecker(TEST_MODULE, packageName, functionName, expectedResult);
        runRhinoTests(testName, ecmaVersions, checker);
    }

    protected void checkFooBoxIsOk(@NotNull String filename) throws Exception {
        checkFooBoxIsOkByPath(getInputFilePath(filename));
    }

    @Override
    protected void checkFooBoxIsOkByPath(@NotNull String filePath) throws Exception {
        runFunctionOutputTestByPath(DEFAULT_ECMA_VERSIONS, filePath, TEST_PACKAGE, TEST_FUNCTION, "OK");
    }

    protected void checkOutput(@NotNull String kotlinFilename,
            @NotNull String expectedResult,
            @NotNull String... args) throws Exception {
        checkOutput(kotlinFilename, expectedResult, DEFAULT_ECMA_VERSIONS, args);
    }

    private void checkOutput(
            @NotNull String kotlinFilename,
            @NotNull String expectedResult,
            @NotNull Iterable<EcmaVersion> ecmaVersions,
            String... args
    ) throws Exception {
        generateJavaScriptFiles(getInputFilePath(kotlinFilename), MainCallParameters.mainWithArguments(Lists.newArrayList(args)), ecmaVersions);
        runRhinoTests(getBaseName(kotlinFilename), ecmaVersions, new RhinoSystemOutputChecker(expectedResult));
    }

    protected void performTestWithMain(@NotNull String testName, @NotNull String testId, @NotNull String... args) throws Exception {
        checkOutput(testName + ".kt", readFile(expectedFilePath(testName + testId)), DEFAULT_ECMA_VERSIONS, args);
    }
}
