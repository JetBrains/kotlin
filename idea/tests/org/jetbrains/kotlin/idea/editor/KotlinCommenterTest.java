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

package org.jetbrains.kotlin.idea.editor;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.testFramework.EditorTestUtil;
import com.intellij.testFramework.LightCodeInsightTestCase;
import org.jetbrains.kotlin.formatter.FormatSettingsUtil;
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase;
import org.jetbrains.kotlin.test.KotlinTestUtils;
import org.jetbrains.kotlin.test.SettingsConfigurator;

import java.io.File;

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

        CodeStyleSettings codeStyleSettings = FormatSettingsUtil.getSettings();
        try {
            String text = myFile.getText();

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
