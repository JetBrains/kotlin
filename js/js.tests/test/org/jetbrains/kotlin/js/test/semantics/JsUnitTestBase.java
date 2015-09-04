/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.js.test.semantics;

import com.google.common.collect.Lists;
import junit.framework.Test;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.js.config.EcmaVersion;
import org.jetbrains.kotlin.js.facade.MainCallParameters;
import org.jetbrains.kotlin.js.test.MultipleFilesTranslationTest;
import org.jetbrains.kotlin.js.test.rhino.RhinoSystemOutputChecker;
import org.jetbrains.kotlin.js.test.rhino.RhinoUtils;
import org.jetbrains.kotlin.js.test.utils.JsUnitTestReporter;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class JsUnitTestBase extends MultipleFilesTranslationTest {

    @NotNull
    private static final String JS_TESTS = TEST_DATA_DIR_PATH + "jsTester/";
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
    protected List<String> additionalJsFiles(@NotNull EcmaVersion ecmaVersion) {
        List<String> result = Lists.newArrayList(super.additionalJsFiles(ecmaVersion));
        result.add(JS_TESTS_JS);
        return result;
    }

    @NotNull
    @Override
    protected List<String> additionalKotlinFiles() {
        List<String> result = StdLibTestBase.removeAdHocAssertions(super.additionalKotlinFiles());
        result.add(JS_TESTS_KT);
        return result;
    }

    @Override
    protected boolean shouldBeTranslateAsUnitTestClass() {
        return true;
    }

    public void runTestFile(@NotNull String pathToTestFile) throws Exception {
        Iterable<EcmaVersion> versions = DEFAULT_ECMA_VERSIONS;
        String testName = pathToTestFile.substring(pathToTestFile.lastIndexOf("/"));

        generateJavaScriptFiles(Collections.singletonList(pathToTestFile), testName, MainCallParameters.noCall(), versions);
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
        Map<String, Object> testVariables = new HashMap<String, Object>();
        testVariables.put(RhinoUtils.OPTIMIZATION_LEVEL_TEST_VARIABLE, RhinoUtils.OPTIMIZATION_OFF);
        testVariables.put("jsTestReporter", JS_UNIT_TEST_REPORTER);
        return testVariables;
    }
}
