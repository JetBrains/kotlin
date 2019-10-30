/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.editor;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.testFramework.EditorTestUtil;
import com.intellij.testFramework.LightCodeInsightTestCase;
import org.jetbrains.kotlin.formatter.FormatSettingsUtil;
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase;
import org.jetbrains.kotlin.test.JUnit3WithIdeaConfigurationRunner;
import org.jetbrains.kotlin.test.KotlinTestUtils;
import org.jetbrains.kotlin.test.SettingsConfigurator;
import org.junit.runner.RunWith;

import java.io.File;
@RunWith(JUnit3WithIdeaConfigurationRunner.class)
public class KotlinCommenterTest extends LightCodeInsightTestCase {
    private static final String BASE_PATH =
            new File(PluginTestCaseBase.getTestDataPathBase(), "/editor/commenter/").getAbsolutePath();

    public void testGenerateDocComment() throws Exception {
        doNewLineTypingTest();
    }

    public void testNewLineInComment() throws Exception {
        doNewLineTypingTest();
    }

    public void testNewLineInTag() throws Exception {
        doNewLineTypingTest();
    }

    public void testNotFirstColumnWithSpace() throws Exception {
        doLineCommentTest();
    }

    public void testNotFirstColumnWithoutSpace() throws Exception {
        doLineCommentTest();
    }

    private void doNewLineTypingTest() throws Exception {
        configure();
        EditorTestUtil.performTypingAction(getEditor(), '\n');
        check();
    }

    private void doLineCommentTest() throws Exception {
        configure();

        CodeStyleSettings codeStyleSettings = FormatSettingsUtil.getSettings(getProject());
        try {
            String text = getFile().getText();

            SettingsConfigurator configurator = FormatSettingsUtil.createConfigurator(text, codeStyleSettings);
            configurator.configureSettings();

            executeAction("CommentByLineComment");
        } finally {
            codeStyleSettings.clearCodeStyleSettings();
        }

        check();
    }

    private void configure() throws Exception {
        configureFromFileText("a.kt", loadFile(getTestName(true) + ".kt"));
    }

    private void check() {
        File afterFile = getTestFile(getTestName(true) + "_after.kt");
        KotlinTestUtils.assertEqualsToFile(afterFile, getEditor(), false);
    }

    private static File getTestFile(String name) {
        return new File(BASE_PATH, name);
    }

    private static String loadFile(String name) throws Exception {
        File file = getTestFile(name);
        return FileUtil.loadFile(file, true);
    }
}
