/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.highlighter;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.analyzer.AnalysisResult;
import org.jetbrains.kotlin.config.CommonConfigurationKeys;
import org.jetbrains.kotlin.config.CommonConfigurationKeysKt;
import org.jetbrains.kotlin.config.CompilerConfiguration;
import org.jetbrains.kotlin.config.LanguageVersionSettings;

import org.jetbrains.kotlin.idea.artifacts.KotlinArtifacts;
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase;
import org.jetbrains.kotlin.js.analyze.TopDownAnalyzerFacadeForJS;
import org.jetbrains.kotlin.js.config.JSConfigurationKeys;
import org.jetbrains.kotlin.js.config.JsConfig;
import org.jetbrains.kotlin.js.resolve.diagnostics.ErrorsJs;
import org.jetbrains.kotlin.psi.KtFile;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public abstract class AbstractDiagnosticMessageJsTest extends AbstractDiagnosticMessageTest {

    protected AnalysisResult analyze(Collection<KtFile> files, CompilerConfiguration configuration) {
        return TopDownAnalyzerFacadeForJS.analyzeFiles(files, getConfig(configuration));
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

    protected CompilerConfiguration compilerConfiguration(LanguageVersionSettings languageVersionSettings) {
        CompilerConfiguration configuration = new CompilerConfiguration();
        configuration.put(CommonConfigurationKeys.MODULE_NAME, myFixture.getModule().getName());
        configuration.put(JSConfigurationKeys.LIBRARIES, jsStdlib());
        configuration.put(CommonConfigurationKeys.DISABLE_INLINE, true);
        CommonConfigurationKeysKt.setLanguageVersionSettings(configuration, languageVersionSettings);
        return configuration;
    }

    protected List<String> jsStdlib() {
        File stdlibPath = KotlinArtifacts.getInstance().getKotlinStdlibJs();
        return Collections.singletonList(stdlibPath.getAbsolutePath());
    }

    @NotNull
    private JsConfig getConfig(CompilerConfiguration configuration) {
        return new JsConfig(getProject(), configuration);
    }
}
