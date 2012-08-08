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

package org.jetbrains.k2js.test.semantics;

import com.google.common.collect.Lists;
import junit.framework.Test;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.k2js.config.Config;
import org.jetbrains.k2js.config.EcmaVersion;
import org.jetbrains.k2js.facade.MainCallParameters;
import org.jetbrains.k2js.test.MultipleFilesTranslationTest;
import org.jetbrains.k2js.test.config.JsUnitTestReporter;
import org.jetbrains.k2js.test.config.TestConfigWithUnitTests;
import org.jetbrains.k2js.test.rhino.RhinoSystemOutputChecker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.jetbrains.k2js.test.utils.LibraryFilePathsUtil.getAdditionalLibraryFiles;

/**
 * @author Pavel Talanov
 */
public abstract class JsUnitTestBase extends MultipleFilesTranslationTest {

    @NotNull
    private static final String JS_TESTS = pathToTestFilesRoot() + "jsTester/";
    @NotNull
    protected static final String JS_TESTS_KT = JS_TESTS + "jsTester.kt";
    @NotNull
    protected static final String JS_TESTS_JS = JS_TESTS + "jsTester.js";
    //NOTE: we use this object to communicate test result from Rhino, it is not necessary to use global objects there
    // we can inject those objects in JavaScript every time but this kind of solution will complicate logic a bit
    @NotNull
    private static final JsUnitTestReporter JS_UNIT_TEST_REPORTER = new JsUnitTestReporter();

    public JsUnitTestBase() {
        super("jsUnitTests/");
    }

    @NotNull
    @Override
    protected List<String> additionalJSFiles(@NotNull EcmaVersion ecmaVersion) {
        ArrayList<String> result = Lists.newArrayList(super.additionalJSFiles(ecmaVersion));
        result.add(JS_TESTS_JS);
        return result;
    }

    @NotNull
    @Override
    protected List<String> additionalKotlinFiles() {
        List<String> result = Lists.newArrayList();
        List<String> additionalLibraryFiles = getAdditionalLibraryFiles();
        additionalLibraryFiles.add(JS_TESTS_KT);
        boolean removed = additionalLibraryFiles.remove(Config.LIBRARIES_LOCATION + "/stdlib/testCode.kt");
        assert removed;
        result.addAll(additionalLibraryFiles);
        return result;
    }

    public void runTestFile(@NotNull String pathToTestFile) throws Exception {
        Iterable<EcmaVersion> versions = failOnEcma5();
        String testName = pathToTestFile.substring(pathToTestFile.lastIndexOf("/"));
        generateJavaScriptFiles(Lists.newArrayList(pathToTestFile), testName, MainCallParameters.noCall(), versions,
                                TestConfigWithUnitTests.FACTORY);
        runRhinoTests(testName, versions, new RhinoSystemOutputChecker(""));
    }

    @NotNull
    public static Test createTestSuiteForFile(@NotNull String file, @NotNull String... ignoreFailedTestCases) throws Exception {
        performTests(file);
        JS_UNIT_TEST_REPORTER.ignoreTests(ignoreFailedTestCases);
        return JS_UNIT_TEST_REPORTER.createTestSuiteAndFlush();
    }

    private static void performTests(@NotNull String testFile) throws Exception {
        //NOTE: well it doesn't
        @SuppressWarnings("JUnitTestCaseWithNoTests") JsUnitTestBase runner = new JsUnitTestBase() {

        };
        try {
            runner.setUp();
            runner.runTestFile(testFile);
        }
        finally {
            runner.tearDown();
        }
    }

    @Override
    protected Map<String, Object> getRhinoTestVariables() throws Exception {
        return Collections.<String, Object>singletonMap("jsTestReporter", JS_UNIT_TEST_REPORTER);
    }
}
