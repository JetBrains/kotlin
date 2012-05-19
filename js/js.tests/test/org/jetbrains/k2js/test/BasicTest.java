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
import com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.k2js.config.EcmaVersion;
import org.jetbrains.k2js.facade.MainCallParameters;
import org.jetbrains.k2js.test.rhino.RhinoResultChecker;
import org.jetbrains.k2js.test.utils.TranslationUtils;

import java.io.File;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import static org.jetbrains.k2js.test.rhino.RhinoUtils.runRhinoTest;
import static org.jetbrains.k2js.test.utils.JsTestUtils.convertFileNameToDotJsFile;

/**
 * @author Pavel Talanov
 */
public abstract class BasicTest extends TestWithEnvironment {

    private static final boolean DELETE_OUT = false;
    private static final String TEST_FILES = "js/js.translator/testFiles/";
    private static final String CASES = "cases/";
    private static final String OUT = "out/";
    private static final String KOTLIN_JS_LIB = pathToTestFilesRoot() + "kotlin_lib.js";
    private static final String EXPECTED = "expected/";

    @NotNull
    private String mainDirectory = "";

    @SuppressWarnings("JUnitTestCaseWithNonTrivialConstructors")
    public BasicTest(@NotNull String main) {
        this.mainDirectory = main;
    }

    @NotNull
    public String getMainDirectory() {
        return mainDirectory;
    }

    protected boolean shouldCreateOut() {
        return true;
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        if (!shouldCreateOut()) {
            return;
        }
        File outDir = new File(getOutputPath());

        JetTestUtils.mkdirs(outDir);
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

    protected List<String> additionalJSFiles() {
        return Collections.singletonList(KOTLIN_JS_LIB);
    }

    protected void generateJavaScriptFiles(@NotNull String kotlinFilename,
            @NotNull MainCallParameters mainCallParameters,
            @NotNull EnumSet<EcmaVersion> ecmaVersions) throws Exception {
        generateJavaScriptFiles(Collections.singletonList(getInputFilePath(kotlinFilename)), kotlinFilename, mainCallParameters,
                                ecmaVersions);
    }

    protected void generateJavaScriptFiles(@NotNull List<String> files, @NotNull String testName,
            @NotNull MainCallParameters mainCallParameters, @NotNull EnumSet<EcmaVersion> ecmaVersions)
            throws Exception {
        for (EcmaVersion version : ecmaVersions) {
            TranslationUtils.translateFiles(getProject(), files, getOutputFilePath(testName, version), mainCallParameters, version);
        }
    }

    protected void runRhinoTests(@NotNull List<String> outputFilePaths, @NotNull RhinoResultChecker checker) throws Exception {
        for (String outputFilePath : outputFilePaths) {
            runRhinoTest(withAdditionalFiles(outputFilePath), checker);
        }
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
    protected String pathToTestFiles() {
        return pathToTestFilesRoot() + getMainDirectory();
    }

    @NotNull
    protected static String pathToTestFilesRoot() {
        return TEST_FILES;
    }

    @NotNull
    private String getOutputPath() {
        return pathToTestFiles() + outDirectoryName();
    }

    @NotNull
    public String getInputPath() {
        return pathToTestFiles() + casesDirectoryName();
    }

    @NotNull
    private String getExpectedPath() {
        return pathToTestFiles() + expectedDirectoryName();
    }

    @NotNull
    protected List<String> withAdditionalFiles(@NotNull String inputFile) {
        List<String> allFiles = Lists.newArrayList(additionalJSFiles());
        allFiles.add(inputFile);
        return allFiles;
    }

    @NotNull
    protected List<String> getOutputFilePaths(@NotNull String filename, @NotNull EnumSet<EcmaVersion> ecmaVersions) {
        List<String> result = Lists.newArrayList();
        for (EcmaVersion ecmaVersion : ecmaVersions) {
            result.add(getOutputFilePath(filename, ecmaVersion));
        }
        return result;
    }

    @NotNull
    protected String getOutputFilePath(@NotNull String filename, @NotNull EcmaVersion ecmaVersion) {
        return getOutputPath() + convertFileNameToDotJsFile(filename, ecmaVersion);
    }

    @NotNull
    protected String getInputFilePath(@NotNull String filename) {
        return getInputPath() + filename;
    }

    @NotNull
    protected String cases(@NotNull String filename) {
        return getInputFilePath(filename);
    }

    @NotNull
    protected String expected(@NotNull String testName) {
        return getExpectedPath() + testName + ".out";
    }
}
