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

import com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.k2js.test.rhino.RhinoFunctionResultChecker;
import org.jetbrains.k2js.test.rhino.RhinoSystemOutputChecker;
import org.jetbrains.k2js.test.utils.TranslationUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.jetbrains.k2js.test.rhino.RhinoUtils.runRhinoTest;
import static org.jetbrains.k2js.test.utils.JsTestUtils.convertToDotJsFile;
import static org.jetbrains.k2js.test.utils.JsTestUtils.readFile;
import static org.jetbrains.k2js.test.utils.TranslationUtils.translateFiles;

//TODO: spread the test* methods amongst classes that actually use them

/**
 * @author Pavel Talanov
 */
@SuppressWarnings("JUnitTestCaseWithNonTrivialConstructors")
public abstract class TranslationTest extends BaseTest {

    private static final boolean DELETE_OUT = false;
    public static final String TEST_FILES = "js.translator/testFiles/";
    private static final String CASES = "cases/";
    private static final String OUT = "out/";
    private static final String KOTLIN_JS_LIB = TEST_FILES + "kotlin_lib.js";
    private static final String EXPECTED = "expected/";

    @NotNull
    private String mainDirectory = "";

    public TranslationTest(@NotNull String main) {
        this.mainDirectory = main;
    }

    @NotNull
    public String getMainDirectory() {
        return mainDirectory;
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        if (!shouldCreateOut()) {
            return;
        }
        File outDir = new File(getOutputPath());
        assert (!outDir.exists() || outDir.isDirectory()) : "If out already exists it should be a directory.";
        if (!outDir.exists()) {
            boolean success = outDir.mkdir();
            assert success;
        }
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        //noinspection ConstantConditions,PointlessBooleanExpression
        if (!shouldCreateOut() || !DELETE_OUT) {
            return;
        }
        File outDir = new File(getOutputPath());
        assert outDir.exists();
        boolean success = FileUtil.delete(outDir);
        assert success;
    }

    protected static String kotlinLibraryPath() {
        return KOTLIN_JS_LIB;
    }

    protected static String casesDirectoryName() {
        return CASES;
    }

    private static String outDirectoryName() {
        return OUT;
    }

    private static String expectedDirectoryName() {
        return EXPECTED;
    }

    @NotNull
    private String pathToTestFiles() {
        return TEST_FILES + getMainDirectory();
    }

    private String getOutputPath() {
        return pathToTestFiles() + outDirectoryName();
    }

    private String getInputPath() {
        return pathToTestFiles() + casesDirectoryName();
    }

    private String getExpectedPath() {
        return pathToTestFiles() + expectedDirectoryName();
    }

    protected void runFunctionOutputTest(String filename, String namespaceName,
                                         String functionName, Object expectedResult) throws Exception {
        generateJsFromFile(filename);
        runRhinoTest(generateFilenameList(getOutputFilePath(filename)),
                     new RhinoFunctionResultChecker(namespaceName, functionName, expectedResult));
    }

    protected void testMultiFile(String dirName, String namespaceName,
                                 String functionName, Object expectedResult) throws Exception {
        generateJsFromDir(dirName);
        runRhinoTest(generateFilenameList(getOutputFilePath(dirName + ".kt")),
                     new RhinoFunctionResultChecker(namespaceName, functionName, expectedResult));
    }

    protected boolean shouldCreateOut() {
        return true;
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

    protected static List<String> generateFilenameList(@NotNull String inputFile) {
        return Arrays.asList(kotlinLibraryPath(), inputFile);
    }

    private String getOutputFilePath(@NotNull String filename) {
        return getOutputPath() + convertToDotJsFile(filename);
    }

    private String getInputFilePath(@NotNull String filename) {
        return getInputPath() + filename;
    }

    protected String cases(@NotNull String filename) {
        return getInputFilePath(filename);
    }

    private String expected(@NotNull String testName) {
        return getExpectedPath() + testName + ".out";
    }

    public void checkFooBoxIsTrue(@NotNull String filename) throws Exception {
        runFunctionOutputTest(filename, "foo", "box", true);
    }

    public void checkFooBoxIsOk(@NotNull String filename) throws Exception {
        runFunctionOutputTest(filename, "foo", "box", "OK");
    }

    protected void checkOutput(String filename, String expectedResult, String... args) throws Exception {
        generateJsFromFile(filename);
        runRhinoTest(generateFilenameList(getOutputFilePath(filename)),
                     new RhinoSystemOutputChecker(expectedResult, Arrays.asList(args)));
    }

    protected void performTestWithMain(String testName, String testId, String... args) throws Exception {
        checkOutput(testName + ".kt", readFile(expected(testName + testId)), args);
    }
}
