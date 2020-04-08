/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.folding;

import com.intellij.codeInsight.folding.JavaCodeFoldingSettings;
import com.intellij.codeInsight.folding.impl.JavaCodeFoldingSettingsImpl;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.rt.execution.junit.FileComparisonFailure;
import com.intellij.testFramework.fixtures.impl.CodeInsightTestFixtureImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase;
import org.jetbrains.kotlin.test.KotlinTestUtils;
import org.jetbrains.kotlin.test.SettingsConfigurator;
import org.junit.Assert;
import org.junit.ComparisonFailure;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.function.Consumer;

public abstract class AbstractKotlinFoldingTest extends KotlinLightCodeInsightFixtureTestCase {
    protected void doTest(@NotNull String path) {
        try {
            myFixture.testFolding(path);
        } catch (FileComparisonFailure e) {
            throw new FileComparisonFailure(e.getMessage(), e.getExpected(), e.getActual(), new File(e.getFilePath()).getAbsolutePath());
        }
    }

    protected void doSettingsFoldingTest(@NotNull String path) throws IOException {
        File file = new File(path);
        String fileText = FileUtil.loadFile(file, true);

        String directText = fileText.replaceAll("~true~", "true").replaceAll("~false~", "false");
        String suffix = "\n\n// Generated from: " + path;
        directText += suffix;

        Consumer<String> doExpandSettingsTestFunction = this::doExpandSettingsTest;

        try {
            doTestWithSettings(directText, doExpandSettingsTestFunction);
        } catch (ComparisonFailure e) {
            KotlinTestUtils.assertEqualsToFile(
                    file,
                    e.getActual().replace(suffix, "")
            );
        }

        String invertedText = fileText
                .replaceAll("~false~", "true").replaceAll("~true~", "false")
                .replaceAll(SettingsConfigurator.SET_TRUE_DIRECTIVE, "~TEMP_TRUE_DIRECTIVE~")
                .replaceAll(SettingsConfigurator.SET_FALSE_DIRECTIVE, SettingsConfigurator.SET_TRUE_DIRECTIVE)
                .replaceAll("~TEMP_TRUE_DIRECTIVE~", SettingsConfigurator.SET_FALSE_DIRECTIVE);
        String invertedSuffix = "\n\n// Generated from: " + path + " with !INVERTED! settings";
        invertedText += invertedSuffix;

        try {
            doTestWithSettings(invertedText, doExpandSettingsTestFunction);
        } catch (ComparisonFailure e) {
            KotlinTestUtils.assertEqualsToFile(
                    file,
                    e.getActual()
                            .replaceAll(SettingsConfigurator.SET_FALSE_DIRECTIVE, "~TEMP_TRUE_DIRECTIVE~")
                            .replaceAll(SettingsConfigurator.SET_TRUE_DIRECTIVE, SettingsConfigurator.SET_FALSE_DIRECTIVE)
                            .replaceAll("~TEMP_TRUE_DIRECTIVE~", SettingsConfigurator.SET_TRUE_DIRECTIVE)
                            .replace(invertedSuffix, "")
            );
        }
    }

    protected static void doTestWithSettings(@NotNull String fileText, @NotNull Consumer<String> runnable) {
        JavaCodeFoldingSettings settings = JavaCodeFoldingSettings.getInstance();
        JavaCodeFoldingSettingsImpl restoreSettings = new JavaCodeFoldingSettingsImpl();
        restoreSettings.loadState((JavaCodeFoldingSettingsImpl) settings);

        try {
            SettingsConfigurator configurator = new SettingsConfigurator(fileText, settings);
            configurator.configureSettings();

            runnable.accept(fileText);
        }
        finally {
            ((JavaCodeFoldingSettingsImpl) JavaCodeFoldingSettings.getInstance()).loadState(restoreSettings);
        }
    }

    private void doExpandSettingsTest(String fileText) {
        try {
            VirtualFile tempFile = createTempFile("kt", null, fileText, Charset.defaultCharset());
            assertFoldingRegionsForFile(tempFile.getPath());
        }
        catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    // Rewritten version of CodeInsightTestFixtureImpl.testFoldingRegions(verificationFileName, true).
    // Configure test with custom file name to force creating different editors for normal and inverted tests.
    private void assertFoldingRegionsForFile(String verificationFileName) {
        String START_FOLD = "<fold\\stext=\'[^\']*\'(\\sexpand=\'[^\']*\')*>";
        String END_FOLD = "</fold>";

        String expectedContent;
        File file = new File(verificationFileName);

        try {
            expectedContent = FileUtil.loadFile(file);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        Assert.assertNotNull(expectedContent);

        expectedContent = StringUtil.replace(expectedContent, "\r", "");
        String cleanContent = expectedContent.replaceAll(START_FOLD, "").replaceAll(END_FOLD, "");

        myFixture.configureByText(file.getName(), cleanContent);
        String actual = ((CodeInsightTestFixtureImpl)myFixture).getFoldingDescription(true);

        Assert.assertEquals(expectedContent, actual);
    }
}
