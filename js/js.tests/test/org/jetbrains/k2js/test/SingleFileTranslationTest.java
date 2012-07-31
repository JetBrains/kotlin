/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.jetbrains.k2js.test;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.k2js.config.EcmaVersion;
import org.jetbrains.k2js.facade.MainCallParameters;
import org.jetbrains.k2js.test.rhino.RhinoFunctionResultChecker;
import org.jetbrains.k2js.test.rhino.RhinoSystemOutputChecker;

import static org.jetbrains.k2js.test.utils.JsTestUtils.readFile;

/**
 * @author Pavel Talanov
 */
@SuppressWarnings("JUnitTestCaseWithNonTrivialConstructors")
public abstract class SingleFileTranslationTest extends BasicTest {
    public SingleFileTranslationTest(@NotNull String main) {
        super(main);
    }

    public void runFunctionOutputTest(@NotNull String kotlinFilename, @NotNull String namespaceName,
            @NotNull String functionName, @NotNull Object expectedResult) throws Exception {
        runFunctionOutputTest(DEFAULT_ECMA_VERSIONS, kotlinFilename, namespaceName, functionName, expectedResult);
    }

    protected void runFunctionOutputTest(@NotNull Iterable<EcmaVersion> ecmaVersions, @NotNull String kotlinFilename,
            @NotNull String namespaceName,
            @NotNull String functionName,
            @NotNull Object expectedResult) throws Exception {
        generateJavaScriptFiles(kotlinFilename, MainCallParameters.noCall(), ecmaVersions);
        runRhinoTests(kotlinFilename, ecmaVersions, new RhinoFunctionResultChecker(namespaceName, functionName, expectedResult));
    }

    public void checkFooBoxIsTrue(@NotNull String filename, @NotNull Iterable<EcmaVersion> ecmaVersions) throws Exception {
        runFunctionOutputTest(ecmaVersions, filename, "foo", "box", true);
    }

    public void checkFooBoxIsTrue(@NotNull String filename) throws Exception {
        runFunctionOutputTest(DEFAULT_ECMA_VERSIONS, filename, "foo", "box", true);
    }

    public void checkFooBoxIsValue(@NotNull String filename, @NotNull Iterable<EcmaVersion> ecmaVersions, Object expected) throws Exception {
        runFunctionOutputTest(ecmaVersions, filename, "foo", "box", expected);
    }

    protected void fooBoxTest() throws Exception {
        checkFooBoxIsTrue(getTestName(true) + ".kt", DEFAULT_ECMA_VERSIONS);
    }

    protected void fooBoxIsValue(Object expected) throws Exception {
        checkFooBoxIsValue(getTestName(true) + ".kt", DEFAULT_ECMA_VERSIONS, expected);
    }

    protected void fooBoxTest(@NotNull Iterable<EcmaVersion> ecmaVersions) throws Exception {
        checkFooBoxIsTrue(getTestName(true) + ".kt", ecmaVersions);
    }

    protected void checkFooBoxIsOk(@NotNull String filename) throws Exception {
        checkFooBoxIsOk(DEFAULT_ECMA_VERSIONS, filename);
    }

    protected void checkFooBoxIsOk(@NotNull Iterable<EcmaVersion> versions, @NotNull String filename) throws Exception {
        runFunctionOutputTest(versions, filename, "foo", "box", "OK");
    }

    protected void checkOutput(@NotNull String kotlinFilename,
            @NotNull String expectedResult,
            @NotNull String... args) throws Exception {
        checkOutput(kotlinFilename, expectedResult, DEFAULT_ECMA_VERSIONS, args);
    }

    protected void checkOutput(@NotNull String kotlinFilename,
            @NotNull String expectedResult,
            @NotNull Iterable<EcmaVersion> ecmaVersions,
            String... args) throws Exception {
        generateJavaScriptFiles(kotlinFilename, MainCallParameters.mainWithArguments(Lists.newArrayList(args)), ecmaVersions);
        runRhinoTests(kotlinFilename, ecmaVersions, new RhinoSystemOutputChecker(expectedResult));
    }

    protected void performTestWithMain(@NotNull Iterable<EcmaVersion> ecmaVersions,
            @NotNull String testName,
            @NotNull String testId,
            @NotNull String... args) throws Exception {
        checkOutput(testName + ".kt", readFile(expected(testName + testId)), ecmaVersions, args);
    }

    protected void performTestWithMain(@NotNull String testName, @NotNull String testId, @NotNull String... args) throws Exception {
        performTestWithMain(DEFAULT_ECMA_VERSIONS, testName, testId, args);
    }
}
