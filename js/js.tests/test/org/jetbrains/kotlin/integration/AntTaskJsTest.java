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

package org.jetbrains.kotlin.integration;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.js.test.rhino.RhinoFunctionResultChecker;
import org.jetbrains.kotlin.js.test.rhino.RhinoUtils;
import org.jetbrains.kotlin.test.KotlinTestUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AntTaskJsTest extends AbstractAntTaskTest {
    private static final String JS_OUT_FILE = "out.js";

    @NotNull
    private String getTestDataDir() {
        return KotlinTestUtils.getTestDataPathBase() + "/integration/ant/js/" + getTestName(true);
    }

    @NotNull
    private File getOutputFileByName(@NotNull String name) {
        return new File(tmpdir, name);
    }

    private void doTest() throws Exception {
        doTest(getTestDataDir());
    }

    private void doJsAntTest(String... jsFiles) throws Exception {
        doJsAntTest(false, jsFiles);
    }

    private void doJsAntTest(boolean withModuleSystem, String... jsFiles) throws Exception {
        doTest();

        List<String> fileNames = new ArrayList<String>(Arrays.asList(jsFiles));
        fileNames.add(JS_OUT_FILE);

        List<String> filePaths = ContainerUtil.map(fileNames, new Function<String, String>() {
            @Override
            public String fun(String s) {
                return getOutputFileByName(s).getAbsolutePath();
            }
        });

        RhinoUtils.runRhinoTest(filePaths, new RhinoFunctionResultChecker("out", "foo", "box", "OK", withModuleSystem));
    }

    private void doJsAntTestForPostfixPrefix(@Nullable String prefix, @Nullable String postfix) throws Exception {
        doJsAntTest();
        File outputFile = getOutputFileByName(JS_OUT_FILE);

        File prefixFile = prefix != null ? new File(getTestDataDir(), prefix) : null;
        File postfixFile = postfix != null ? new File(getTestDataDir(), postfix) : null;

        checkFilePrefixPostfix(outputFile, prefixFile, postfixFile);
    }

    private static void checkFilePrefixPostfix(@NotNull File file, @Nullable File prefix, @Nullable File postfix) throws IOException {
        String fileContent = FileUtil.loadFile(file, true);

        if (prefix != null) {
            String prefixContent = FileUtil.loadFile(prefix, true);
            assertTrue(fileContent.startsWith(prefixContent));
        }

        if (postfix != null) {
            String postfixContent = FileUtil.loadFile(postfix, true);
            assertTrue(fileContent.endsWith(postfixContent));
        }
    }

    public void testSimple() throws Exception {
        doJsAntTest();
    }

    public void testSimpleWithMain() throws Exception {
        doJsAntTest();
    }

    public void testSimpleWithStdlib() throws Exception {
        doJsAntTest();
    }

    public void testSimpleWithoutStdlibAndFolderAsAnotherLib() throws Exception {
        doJsAntTest("jslib-example.js");
    }

    public void testSimpleWithoutStdlibAndJsFileAsAnotherLib() throws Exception {
        doJsAntTest("jslib-example.js");
    }

    public void testSimpleWithStdlibAndJsFileAsAnotherLib() throws Exception {
        doJsAntTest("jslib-example.js");
    }

    public void testSimpleWithStdlibAndJsFileAsAnotherLibModuleKind() throws Exception {
        doJsAntTest(true, "amd.js", "jslib-example.js");
    }

    public void testSimpleWithStdlibAndTwoJsFilesAsLibraries() throws Exception {
        doJsAntTest("jslib-example1.js", "jslib-example2.js");
    }

    public void testSimpleWithStdlibAndJsFilesWithTwoModulesAsLibrary() throws Exception {
        doJsAntTest("jslib-example.js");
    }

    public void testSimpleWithMainFQArgs() throws Exception {
        doJsAntTest();
    }

    public void testSimpleWithVarargMain() throws Exception {
        doJsAntTest();
    }

    public void testManySources() throws Exception {
        doJsAntTest();
    }

    public void testAdditionalArguments() throws Exception {
        doJsAntTest();
    }

    public void testSuppressWarnings() throws Exception {
        doJsAntTest();
    }

    public void testVerbose() throws Exception {
        doJsAntTest();
    }

    public void testVersion() throws Exception {
        doJsAntTest();
    }

    public void testOutputWithoutDirectory() throws Exception {
        doJsAntTest();
    }

    public void testNoSrcParam() throws Exception {
        doTest();
    }

    public void testNoOutputParam() throws Exception {
        doTest();
    }

    public void testOutputPrefix() throws Exception {
        doJsAntTestForPostfixPrefix("prefix", null);
    }

    public void testOutputPostfix() throws Exception {
        doJsAntTestForPostfixPrefix(null, "postfix");
    }

    public void testBothPrefixAndPostfix() throws Exception {
        doJsAntTestForPostfixPrefix("prefix", "postfix");
    }

    public void testSourceMap() throws Exception {
        doJsAntTest();

        File sourceMap = getOutputFileByName(JS_OUT_FILE + ".map");
        assertTrue("Source map file \"" + sourceMap.getAbsolutePath() + "\" not found", sourceMap.exists());
    }
}
