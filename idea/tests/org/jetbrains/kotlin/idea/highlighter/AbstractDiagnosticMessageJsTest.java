/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
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
        configuration.put(JSConfigurationKeys.LIBRARIES, JsConfig.JS_STDLIB);
        configuration.put(CommonConfigurationKeys.DISABLE_INLINE, true);
        if (explicitLanguageVersion != null) {
            CommonConfigurationKeysKt.setLanguageVersionSettings(
                    configuration,
                    new LanguageVersionSettingsImpl(explicitLanguageVersion, LanguageVersionSettingsImpl.DEFAULT.getApiVersion())
            );
        }
        return new JsConfig(getProject(), configuration);
    }
}
