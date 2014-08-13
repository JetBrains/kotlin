/*
 * Copyright 2010-2013 JetBrains s.r.o.
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
import com.intellij.util.Consumer;
import junit.framework.Test;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.k2js.config.Config;
import org.jetbrains.k2js.config.EcmaVersion;
import org.jetbrains.k2js.facade.MainCallParameters;
import org.jetbrains.k2js.test.semantics.TranslatorTestCaseBuilder;
import org.jetbrains.k2js.test.utils.TranslationUtils;

import java.io.File;
import java.util.List;

@SuppressWarnings("JUnitTestCaseWithNoTests")
public final class OutputPrefixPostfixTest extends SingleFileTranslationTest {
    private static final String PREFIX_EXT = ".prefix";
    private static final String POSTFIX_EXT = ".postfix";

    @NotNull
    private final String filename;
    private final File outputPrefixFile;
    private final File outputPostfixFile;

    @SuppressWarnings("JUnitTestCaseWithNonTrivialConstructors")
    public OutputPrefixPostfixTest(@NotNull String filename) {
        super("outputPrefixPostfix/");
        this.filename = filename;

        String inputFilePath = getInputFilePath(filename);
        this.outputPrefixFile = newFileIfExists(inputFilePath + PREFIX_EXT);
        this.outputPostfixFile = newFileIfExists(inputFilePath + POSTFIX_EXT);
    }

    @Override
    protected void translateFiles(
            @NotNull List<JetFile> jetFiles,
            @NotNull File outputFile,
            @NotNull MainCallParameters mainCallParameters,
            @NotNull Config config
    ) throws Exception {
        //noinspection unchecked
        TranslationUtils.translateFiles(mainCallParameters, jetFiles, outputFile, outputPrefixFile, outputPostfixFile, config, Consumer.EMPTY_CONSUMER);
    }

    @Override
    public void runTest() throws Exception {
        checkFooBoxIsOk(filename);
    }

    @Override
    protected void runFunctionOutputTest(
            @NotNull Iterable<EcmaVersion> ecmaVersions,
            @NotNull String kotlinFilename,
            @NotNull String packageName,
            @NotNull String functionName,
            @NotNull Object expectedResult
    ) throws Exception {
        super.runFunctionOutputTest(ecmaVersions, kotlinFilename, packageName, functionName, expectedResult);

        for (EcmaVersion ecmaVersion : ecmaVersions) {
            String output = FileUtil.loadFile(new File(getOutputFilePath(filename, ecmaVersion)), true);
            if (outputPrefixFile != null) {
                assertTrue(output.startsWith(FileUtil.loadFile(outputPrefixFile, true)));
            }
            if (outputPostfixFile != null) {
                assertTrue(output.endsWith(FileUtil.loadFile(outputPostfixFile, true)));
            }
        }
    }

    public static Test suite() throws Exception {
        return TranslatorTestCaseBuilder
                .suiteForDirectory(BasicTest.pathToTestFilesRoot() + "outputPrefixPostfix/cases/", new TranslatorTestCaseBuilder.NamedTestFactory() {
                    @NotNull
                    @Override
                    public Test createTest(@NotNull String filename) {
                        OutputPrefixPostfixTest examplesTest = new OutputPrefixPostfixTest(filename);
                        examplesTest.setName(filename);
                        return examplesTest;
                    }
                });
    }

    @Nullable
    private static File newFileIfExists(@NotNull String path) {
        File file = new File(path);
        if (!file.exists()) {
            file = null;
        }

        return file;
    }
}
