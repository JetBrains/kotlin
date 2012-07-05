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

import closurecompiler.internal.com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.k2js.config.Config;
import org.jetbrains.k2js.config.EcmaVersion;
import org.jetbrains.k2js.facade.MainCallParameters;
import org.jetbrains.k2js.test.MultipleFilesTranslationTest;
import org.jetbrains.k2js.test.config.TestConfigWithUnitTests;
import org.jetbrains.k2js.test.rhino.RhinoSystemOutputChecker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.k2js.test.utils.LibraryFilePathsUtil.getAdditionalLibraryFiles;

/**
 * @author Pavel Talanov
 */
public class JsUnitTestBase extends MultipleFilesTranslationTest {

    @NotNull
    private static final String JS_TESTS = pathToTestFilesRoot() + "jsTester/";
    @NotNull
    protected static final String JS_TESTS_KT = JS_TESTS + "jsTester.kt";
    @NotNull
    protected static final String JS_TESTS_JS = JS_TESTS + "jsTester.js";

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
        ArrayList<String> result = Lists.newArrayList();
        List<String> additionalLibraryFiles = getAdditionalLibraryFiles();
        additionalLibraryFiles.add(JS_TESTS_KT);
        boolean removed = additionalLibraryFiles.remove(Config.LIBRARIES_LOCATION + "/stdlib/testCode.kt");
        assert removed;
        result.addAll(additionalLibraryFiles);
        return result;
    }

    public void testDummy() throws Exception {
        performUnitTest("libraries/stdlib/test/ListTest.kt");
    }

    private void performUnitTest(String... testFiles) throws Exception {
        Iterable<EcmaVersion> versions = Collections.singletonList(EcmaVersion.v3);
        generateJavaScriptFiles(Lists.newArrayList(testFiles), "myTest", MainCallParameters.noCall(), versions,
                                TestConfigWithUnitTests.FACTORY);
        runRhinoTests("myTest", versions, new RhinoSystemOutputChecker(""));
    }
}
