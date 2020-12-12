/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.integration;

import com.intellij.openapi.util.io.FileUtil;
import kotlin.collections.CollectionsKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.js.test.NashornJsTestChecker;
import org.jetbrains.kotlin.js.test.V8JsTestChecker;
import org.jetbrains.kotlin.test.KotlinTestUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AntTaskJsTest extends AbstractAntTaskTest {
    private static final String JS_OUT_FILE = "out.js";
    private static final Boolean useNashorn = Boolean.getBoolean("kotlin.js.useNashorn");

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

        List<String> fileNames = new ArrayList<>(Arrays.asList(jsFiles));
        fileNames.add(JS_OUT_FILE);

        List<String> filePaths = CollectionsKt.map(fileNames, s -> getOutputFileByName(s).getAbsolutePath());

        (useNashorn ? NashornJsTestChecker.INSTANCE : V8JsTestChecker.INSTANCE).check(filePaths, "out", "foo", "box", "OK", withModuleSystem);
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

    public void testSimpleWithoutStdlib() throws Exception {
        doJsAntTest();
    }

    public void testSimpleWithoutStdlibAndFolderAsAnotherLib() throws Exception {
        doJsAntTest("jslib-example.js");
    }

    public void testSimpleWithoutStdlibAndJsFileAsAnotherLib() throws Exception {
        doJsAntTest("jslib-example.js");
    }

    public void testSimpleWithJsFileAsAnotherLib() throws Exception {
        doJsAntTest("jslib-example.js");
    }

    public void testSimpleWithJsFileAsAnotherLibModuleKind() throws Exception {
        doJsAntTest(true, "amd.js", "jslib-example.js");
    }

    public void testSimpleWithTwoJsFilesAsLibraries() throws Exception {
        doJsAntTest("jslib-example1.js", "jslib-example2.js");
    }

    public void testSimpleWithJsFilesWithTwoModulesAsLibrary() throws Exception {
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
