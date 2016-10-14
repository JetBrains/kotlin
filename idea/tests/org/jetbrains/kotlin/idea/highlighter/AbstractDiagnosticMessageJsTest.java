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
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment;
import org.jetbrains.kotlin.config.*;
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase;
import org.jetbrains.kotlin.js.analyze.TopDownAnalyzerFacadeForJS;
import org.jetbrains.kotlin.js.config.JSConfigurationKeys;
import org.jetbrains.kotlin.js.config.JsConfig;
import org.jetbrains.kotlin.js.config.LibrarySourcesConfig;
import org.jetbrains.kotlin.js.resolve.diagnostics.ErrorsJs;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.test.KotlinTestUtils;

import java.lang.reflect.Field;

import static java.util.Collections.singletonList;

public abstract class AbstractDiagnosticMessageJsTest extends AbstractDiagnosticMessageTest {
    @NotNull
    @Override
    protected KotlinCoreEnvironment createEnvironment() {
        return KotlinCoreEnvironment.createForTests(getTestRootDisposable(), new CompilerConfiguration(), EnvironmentConfigFiles.JS_CONFIG_FILES);
    }

    @NotNull
    @Override
    protected AnalysisResult analyze(@NotNull KtFile file, @Nullable LanguageVersion explicitLanguageVersion) {
        return TopDownAnalyzerFacadeForJS.analyzeFiles(singletonList(file), getConfig(explicitLanguageVersion));
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
    private JsConfig getConfig(@Nullable LanguageVersion explicitLanguageVersion) {
        CompilerConfiguration configuration = getEnvironment().getConfiguration().copy();
        configuration.put(CommonConfigurationKeys.MODULE_NAME, KotlinTestUtils.TEST_MODULE_NAME);
        configuration.put(JSConfigurationKeys.LIBRARY_FILES, LibrarySourcesConfig.JS_STDLIB);
        configuration.put(CommonConfigurationKeys.DISABLE_INLINE, true);
        configuration.put(JSConfigurationKeys.UNIT_TEST_CONFIG, true);
        if (explicitLanguageVersion != null) {
            configuration.put(CommonConfigurationKeys.LANGUAGE_VERSION_SETTINGS,
                              new LanguageVersionSettingsImpl(explicitLanguageVersion, ApiVersion.LATEST));
        }
        return new LibrarySourcesConfig(getProject(), configuration);
    }
}
