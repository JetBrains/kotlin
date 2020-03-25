/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.highlighter;

import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.analyzer.AnalysisResult;
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment;
import org.jetbrains.kotlin.config.*;
import org.jetbrains.kotlin.diagnostics.Diagnostic;
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory;
import org.jetbrains.kotlin.diagnostics.Errors;
import org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages;
import org.jetbrains.kotlin.idea.highlighter.formatHtml.FormatHtmlUtilKt;
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ErrorsJvm;
import org.jetbrains.kotlin.resolve.lazy.JvmResolveUtil;
import org.jetbrains.kotlin.test.*;

import java.io.File;
import java.lang.reflect.Field;
import java.util.*;

public abstract class AbstractDiagnosticMessageTest extends KotlinTestWithEnvironment {
    private static final String DIAGNOSTICS_NUMBER_DIRECTIVE = "DIAGNOSTICS_NUMBER";
    private static final String DIAGNOSTICS_DIRECTIVE = "DIAGNOSTICS";
    private static final String MESSAGE_TYPE_DIRECTIVE = "MESSAGE_TYPE";

    private enum MessageType {
        TEXT("TEXT", "txt"), HTML("HTML", "html");

        public final String directive;
        public final String extension;

        MessageType(String directive, String extension) {
            this.directive = directive;
            this.extension = extension;
        }
    }

    @NotNull
    @Override
    protected KotlinCoreEnvironment createEnvironment() {
        return createEnvironmentWithMockJdk(ConfigurationKind.ALL);
    }

    @NotNull
    protected String getTestDataPath() {
        return PluginTestCaseBase.getTestDataPathBase() + "/diagnosticMessage/";
    }

    @NotNull
    protected AnalysisResult analyze(@NotNull KtFile file, @Nullable LanguageVersion explicitLanguageVersion, @NotNull Map<LanguageFeature, LanguageFeature.State> specificFeatures) {
        CompilerConfiguration configuration = getEnvironment().getConfiguration();
        LanguageVersion languageVersion = explicitLanguageVersion == null
                                          ? CommonConfigurationKeysKt.getLanguageVersionSettings(configuration).getLanguageVersion()
                                          : explicitLanguageVersion;

        CommonConfigurationKeysKt.setLanguageVersionSettings(configuration, new LanguageVersionSettingsImpl(
                languageVersion,
                LanguageVersionSettingsImpl.DEFAULT.getApiVersion(),
                Collections.emptyMap(),
                specificFeatures));
        return JvmResolveUtil.analyze(Collections.singleton(file), getEnvironment(), configuration);
    }

    public void doTest(String filePath) throws Exception {
        File file = new File(filePath);
        String fileName = file.getName();

        String fileData = KotlinTestUtils.doLoadFile(file);
        Directives directives = KotlinTestUtils.parseDirectives(fileData);
        int diagnosticNumber = getDiagnosticNumber(directives);
        final Set<DiagnosticFactory<?>> diagnosticFactories = getDiagnosticFactories(directives);
        MessageType messageType = getMessageTypeDirective(directives);

        String explicitLanguageVersion = InTextDirectivesUtils.findStringWithPrefixes(fileData, "// LANGUAGE_VERSION:");
        LanguageVersion version = explicitLanguageVersion == null ? null : LanguageVersion.fromVersionString(explicitLanguageVersion);
        Map<LanguageFeature, LanguageFeature.State> specificFeatures = parseLanguageFeatures(fileData);

        KtFile psiFile = KotlinTestUtils.createFile(fileName, KotlinTestUtils.doLoadFile(getTestDataPath(), fileName), getProject());
        AnalysisResult analysisResult = analyze(psiFile, version, specificFeatures);
        BindingContext bindingContext = analysisResult.getBindingContext();

        List<Diagnostic> diagnostics = ContainerUtil.filter(bindingContext.getDiagnostics().all(), new Condition<Diagnostic>() {
            @Override
            public boolean value(Diagnostic diagnostic) {
                return diagnosticFactories.contains(diagnostic.getFactory());
            }
        });

        assertEquals("Expected diagnostics number mismatch:", diagnosticNumber, diagnostics.size());

        int index = 1;
        String name = FileUtil.getNameWithoutExtension(fileName);
        for (Diagnostic diagnostic : diagnostics) {
            String readableDiagnosticText;
            String extension;
            if (messageType != MessageType.TEXT && IdeErrorMessages.hasIdeSpecificMessage(diagnostic)) {
                readableDiagnosticText = FormatHtmlUtilKt.formatHtml(IdeErrorMessages.render(diagnostic));
                extension = MessageType.HTML.extension;
            }
            else {
                readableDiagnosticText = DefaultErrorMessages.render(diagnostic);
                extension = MessageType.TEXT.extension;
            }
            String errorMessageFileName = name + index;
            String path = getTestDataPath() + "/" + errorMessageFileName + "." + extension;
            String actualText = "<!-- " + errorMessageFileName + " -->\n" + readableDiagnosticText;
            assertSameLinesWithFile(path, actualText);

            index++;
        }
    }

    private Map<LanguageFeature, LanguageFeature.State> parseLanguageFeatures(String fileText) {
        List<String> directives = InTextDirectivesUtils.findListWithPrefixes(fileText, "// !LANGUAGE:");
        Map<LanguageFeature, LanguageFeature.State> result = new HashMap<>();
        for (String directive : directives) {
            LanguageFeature.State state;
            char sign = directive.charAt(0);
            switch (sign) {
                case '+':
                    state = LanguageFeature.State.ENABLED;
                    break;
                case '-':
                    state = LanguageFeature.State.DISABLED;
                    break;
                default:
                    continue;
            }
            String featureName = directive.substring(1);
            LanguageFeature feature;
            try {
                feature = LanguageFeature.fromString(featureName);
            } catch (Exception e) {
                continue;
            }
            result.put(feature, state);
        }
        return result;
    }

    private static int getDiagnosticNumber(Directives directives) {
        String diagnosticsNumber = directives.get(DIAGNOSTICS_NUMBER_DIRECTIVE);
        assert diagnosticsNumber != null : DIAGNOSTICS_NUMBER_DIRECTIVE + " should be present.";
        try {
            return Integer.parseInt(diagnosticsNumber);
        }
        catch (NumberFormatException e) {
            throw new AssertionError(DIAGNOSTICS_NUMBER_DIRECTIVE + " should contain number as its value.");
        }
    }

    @NotNull
    private Set<DiagnosticFactory<?>> getDiagnosticFactories(Directives directives) {
        String diagnosticsData = directives.get(DIAGNOSTICS_DIRECTIVE);
        assert diagnosticsData != null : DIAGNOSTICS_DIRECTIVE + " should be present.";
        Set<DiagnosticFactory<?>> diagnosticFactories = new HashSet<>();
        String[] diagnostics = diagnosticsData.split(" ");
        for (String diagnosticName : diagnostics) {
            Object diagnostic = getDiagnostic(diagnosticName);
            assert diagnostic instanceof DiagnosticFactory: "Can't load diagnostic factory for " + diagnosticName;
            diagnosticFactories.add((DiagnosticFactory) diagnostic);
        }
        return diagnosticFactories;
    }

    @Nullable
    private Object getDiagnostic(@NotNull String diagnosticName) {
        Field field = getPlatformSpecificDiagnosticField(diagnosticName);

        if (field == null) {
            field = getFieldOrNull(Errors.class, diagnosticName);
        }

        if (field == null) return null;

        try {
            return field.get(null);
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    @Nullable
    protected Field getPlatformSpecificDiagnosticField(@NotNull String diagnosticName) {
        return getFieldOrNull(ErrorsJvm.class, diagnosticName);
    }

    @Nullable
    protected static Field getFieldOrNull(@NotNull Class<?> kind, @NotNull String field) {
        try {
            return kind.getField(field);
        } catch (NoSuchFieldException e) {
            return null;
        }
    }

    @Nullable
    private static MessageType getMessageTypeDirective(Directives directives) {
        String messageType = directives.get(MESSAGE_TYPE_DIRECTIVE);
        if (messageType == null) return null;
        try {
            return MessageType.valueOf(messageType);
        }
        catch (IllegalArgumentException e) {
            throw new AssertionError(MESSAGE_TYPE_DIRECTIVE + " should be " + MessageType.TEXT.directive + " or " +
                                     MessageType.HTML.directive + ". But was: \"" + messageType + "\".");
        }
    }
}
