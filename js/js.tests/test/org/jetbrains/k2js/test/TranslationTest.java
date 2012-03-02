/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.k2js.test.rhino.RhinoFunctionResultChecker;
import org.jetbrains.k2js.test.rhino.RhinoSystemOutputChecker;
import org.jetbrains.k2js.test.utils.TranslationUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.jetbrains.k2js.test.rhino.RhinoUtils.runRhinoTest;
import static org.jetbrains.k2js.test.utils.JsTestUtils.readFile;
import static org.jetbrains.k2js.test.utils.TranslationUtils.translateFiles;

/**
 * @author Pavel Talanov
 */
@SuppressWarnings("JUnitTestCaseWithNonTrivialConstructors")
public abstract class TranslationTest extends BasicTest {

    public TranslationTest(@NotNull String main) {
        super(main);
    }

    protected void runFunctionOutputTest(String filename, String namespaceName,
                                         String functionName, Object expectedResult) throws Exception {
        generateJsFromFile(filename);
        runRhinoTest(withKotlinJsLib(getOutputFilePath(filename)),
                     new RhinoFunctionResultChecker(namespaceName, functionName, expectedResult));
    }

    protected void runMultiFileTest(String dirName, String namespaceName,
                                    String functionName, Object expectedResult) throws Exception {
        generateJsFromDir(dirName);
        runRhinoTest(withKotlinJsLib(getOutputFilePath(dirName + ".kt")),
                     new RhinoFunctionResultChecker(namespaceName, functionName, expectedResult));
    }

    protected void generateJsFromFile(@NotNull String filename) throws Exception {
        TranslationUtils.translateFile(getProject(), getInputFilePath(filename), getOutputFilePath(filename));
    }

    protected void generateJsFromDir(@NotNull String dirName) throws Exception {
        String dirPath = getInputFilePath(dirName);
        File dir = new File(dirPath);
        List<String> fullFilePaths = new ArrayList<String>();
        for (String fileName : dir.list()) {
            fullFilePaths.add(getInputFilePath(dirName) + "/" + fileName);
        }
        assert dir.isDirectory();
        translateFiles(getProject(), fullFilePaths, getOutputFilePath(dirName + ".kt"));
    }


    public void checkFooBoxIsTrue(@NotNull String filename) throws Exception {
        runFunctionOutputTest(filename, "foo", "box", true);
    }

    public void checkFooBoxIsOk(@NotNull String filename) throws Exception {
        runFunctionOutputTest(filename, "foo", "box", "OK");
    }

    protected void checkOutput(@NotNull String filename, @NotNull String expectedResult, @NotNull String... args) throws Exception {
        generateJsFromFile(filename);
        runRhinoTest(withKotlinJsLib(getOutputFilePath(filename)),
                     new RhinoSystemOutputChecker(expectedResult, Arrays.asList(args)));
    }

    protected void performTestWithMain(@NotNull String testName, @NotNull String testId, @NotNull String... args) throws Exception {
        checkOutput(testName + ".kt", readFile(expected(testName + testId)), args);
    }
}
