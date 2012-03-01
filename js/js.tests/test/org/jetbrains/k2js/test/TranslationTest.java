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

import com.google.dart.compiler.backend.js.ast.JsProgram;
import com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.k2js.facade.K2JSTranslator;
import sun.org.mozilla.javascript.internal.Context;
import sun.org.mozilla.javascript.internal.Scriptable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.k2js.utils.JetFileUtils.createPsiFileList;

//TODO: spread the test* methods amongst classes that actually use them

/**
 * @author Pavel Talanov
 */
public abstract class TranslationTest extends BaseTest {

    private static final boolean DELETE_OUT = false;
    public static final String TEST_FILES = "js.translator/testFiles/";
    private static final String CASES = "cases/";
    private static final String OUT = "out/";
    private static final String KOTLIN_JS_LIB = TEST_FILES + "kotlin_lib.js";
    private static final String EXPECTED = "expected/";

    public void translateFile(@NotNull String inputFile,
                              @NotNull String outputFile) throws Exception {
        translateFiles(Collections.singletonList(inputFile), outputFile);
    }

    public void translateFiles(@NotNull List<String> inputFiles,
                               @NotNull String outputFile) throws Exception {
        K2JSTranslator translator = new K2JSTranslator(new TestConfig(getProject()));
        List<JetFile> psiFiles = createPsiFileList(inputFiles, getProject());
        JsProgram program = translator.generateProgram(psiFiles);
        K2JSTranslator.saveProgramToFile(outputFile, program);
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

    protected abstract String mainDirectory();

    private String pathToTestFiles() {
        return TEST_FILES + suiteDirectoryName() + mainDirectory();
    }

    protected String suiteDirectoryName() {
        return "";
    }

    private String getOutputPath() {
        return pathToTestFiles() + outDirectoryName();
    }

    protected String getInputPath() {
        return pathToTestFiles() + casesDirectoryName();
    }

    private String getExpectedPath() {
        return pathToTestFiles() + expectedDirectoryName();
    }

    @SuppressWarnings("MethodMayBeStatic")
    private String expectedDirectoryName() {
        return EXPECTED;
    }

    protected void testFunctionOutput(String filename, String namespaceName,
                                      String functionName, Object expectedResult) throws Exception {
        translateFile(filename);
        runRhinoTest(generateFilenameList(getOutputFilePath(filename)),
                     new RhinoFunctionResultChecker(namespaceName, functionName, expectedResult));
    }

    protected void testMultiFile(String dirName, String namespaceName,
                                 String functionName, Object expectedResult) throws Exception {
        translateFilesInDir(dirName);
        runRhinoTest(generateFilenameList(getOutputFilePath(dirName + ".kt")),
                     new RhinoFunctionResultChecker(namespaceName, functionName, expectedResult));
    }

    protected boolean shouldCreateOut() {
        return true;
    }

    protected void translateFile(String filename) throws Exception {
        translateFile(getInputFilePath(filename),
                      getOutputFilePath(filename));
    }

    protected void translateFilesInDir(String dirName) throws Exception {
        String dirPath = getInputFilePath(dirName);
        File dir = new File(dirPath);
        List<String> fullFilePaths = new ArrayList<String>();
        for (String fileName : dir.list()) {
            fullFilePaths.add(getInputFilePath(dirName) + "/" + fileName);
        }
        assert dir.isDirectory();
        translateFiles(fullFilePaths,
                       getOutputFilePath(dirName + ".kt"));
    }

    protected List<String> generateFilenameList(String inputFile) {
        return Arrays.asList(kotlinLibraryPath(), inputFile);
    }

    //TODO: refactor filename generation logic
    protected String getOutputFilePath(String filename) {
        return getOutputPath() + convertToDotJsFile(filename);
    }

    private String convertToDotJsFile(String filename) {
        return filename.substring(0, filename.lastIndexOf('.')) + ".js";
    }

    private String getInputFilePath(String filename) {
        return getInputPath() + filename;
    }

    protected String cases(String filename) {
        return getInputFilePath(filename);
    }

    private String expected(String testName) {
        return getExpectedPath() + testName + ".out";
    }

    protected static void runFileWithRhino(String inputFile, Context context, Scriptable scope) throws Exception {
        FileReader reader = new FileReader(inputFile);
        try {
            context.evaluateReader(scope, reader, inputFile, 1, null);
        } finally {
            reader.close();
        }
    }

    protected static void runRhinoTest(@NotNull List<String> fileNames,
                                       @NotNull RhinoResultChecker checker) throws Exception {
        Context context = Context.enter();
        Scriptable scope = context.initStandardObjects();
        for (String filename : fileNames) {
            runFileWithRhino(filename, context, scope);
        }
        checker.runChecks(context, scope);
        Context.exit();
    }

    public void checkFooBoxIsTrue(String filename) throws Exception {
        testFunctionOutput(filename, "foo", "box", true);
    }

    public void checkFooBoxIsOk(String filename) throws Exception {
        testFunctionOutput(filename, "foo", "box", "OK");
    }

    protected void checkOutput(String filename, String expectedResult, String... args) throws Exception {
        translateFile(filename);
        runRhinoTest(generateFilenameList(getOutputFilePath(filename)),
                     new RhinoSystemOutputChecker(expectedResult, Arrays.asList(args)));
    }

    protected void performTestWithMain(String testName, String testId, String... args) throws Exception {
        checkOutput(testName + ".kt", readFile(expected(testName + testId)), args);
    }

    private static String readFile(String path) throws IOException {
        FileInputStream stream = new FileInputStream(new File(path));
        try {
            return FileUtil.loadTextAndClose(stream);
        } finally {
            stream.close();
        }
    }
}
