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

package org.jetbrains.k2js.test.semantics;

import com.intellij.openapi.project.Project;
import junit.framework.Test;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.k2js.config.EcmaVersion;
import org.jetbrains.k2js.facade.K2JSTranslator;
import org.jetbrains.k2js.facade.MainCallParameters;
import org.jetbrains.k2js.test.BasicTest;
import org.jetbrains.k2js.test.SingleFileTranslationTest;
import org.jetbrains.k2js.test.config.TestConfig;
import org.jetbrains.k2js.test.config.TestConfigFactory;

import java.util.List;

import static org.jetbrains.k2js.test.utils.TranslationUtils.createJetFileList;
import static org.jetbrains.k2js.test.utils.TranslationUtils.getConfig;

@SuppressWarnings("JUnitTestCaseWithNoTests")
public final class SourcemapTest extends SingleFileTranslationTest {

    @NotNull
    private final String filename;

    @SuppressWarnings("JUnitTestCaseWithNonTrivialConstructors")
    public SourcemapTest(@NotNull String filename) {
        super("sourcemap/");
        this.filename = filename;
    }

    @Override
    protected void translateFiles(
            @NotNull Project project,
            @NotNull List<String> files,
            @NotNull String outputFile,
            @NotNull MainCallParameters mainCallParameters,
            @NotNull EcmaVersion version,
            @NotNull TestConfigFactory configFactory
    ) throws Exception {
        K2JSTranslator.translateWithMainCallParametersAndSaveToFile(mainCallParameters, createJetFileList(project, files, null), outputFile,
                                                                    getConfig(project, version, configFactory));
    }

    @NotNull
    @Override
    protected TestConfigFactory getConfigFactory() {
        return TestConfig.FACTORY_WITH_SOURCEMAP;
    }

    @Override
    public void runTest() throws Exception {
        checkFooBoxIsOk(filename);
    }

    public static Test suite() throws Exception {
        return TranslatorTestCaseBuilder
                .suiteForDirectory(BasicTest.pathToTestFilesRoot() + "sourcemap/cases/", new TranslatorTestCaseBuilder.NamedTestFactory() {
                    @NotNull
                    @Override
                    public Test createTest(@NotNull String filename) {
                        SourcemapTest examplesTest = new SourcemapTest(filename);
                        examplesTest.setName(filename);
                        return examplesTest;
                    }
                });
    }
}
