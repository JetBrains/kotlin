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

package org.jetbrains.kotlin.idea.highlighter;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.analyzer.AnalysisResult;
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles;
import org.jetbrains.kotlin.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.kotlin.config.CompilerConfiguration;
import org.jetbrains.kotlin.idea.PluginTestCaseBase;
import org.jetbrains.kotlin.js.analyze.TopDownAnalyzerFacadeForJS;
import org.jetbrains.kotlin.js.config.Config;
import org.jetbrains.kotlin.js.config.EcmaVersion;
import org.jetbrains.kotlin.js.config.LibrarySourcesConfigWithCaching;
import org.jetbrains.kotlin.js.resolve.diagnostics.ErrorsJs;
import org.jetbrains.kotlin.psi.JetFile;

import java.lang.reflect.Field;

import static java.util.Collections.singletonList;

public abstract class AbstractDiagnosticMessageJsTest extends AbstractDiagnosticMessageTest {
    @NotNull
    @Override
    protected JetCoreEnvironment createEnvironment() {
        return JetCoreEnvironment.createForTests(getTestRootDisposable(), new CompilerConfiguration(), EnvironmentConfigFiles.JS_CONFIG_FILES);
    }

    @NotNull
    @Override
    protected AnalysisResult analyze(@NotNull JetFile file) {
        return TopDownAnalyzerFacadeForJS.analyzeFiles(singletonList(file), getConfig());
    }

    @NotNull
    @Override
    protected String getTestDataPath() {
        return PluginTestCaseBase.getTestDataPathBase() + "/diagnosticMessage/js/";
    }

    @Nullable
    @Override
    protected Field getPlatformSpecificDiagnosticField(@NotNull String diagnosticName) {
        return getFieldOrNull(ErrorsJs.class, diagnosticName);
    }

    @NotNull
    private Config getConfig() {
        return new LibrarySourcesConfigWithCaching(getProject(),
                                                   "testModule",
                                                   EcmaVersion.defaultVersion(),
                                                   /* sourceMap = */ false,
                                                   /* inlineEnabled = */ false,
                                                   /* isUnitTestMode = */ true);
    }
}
