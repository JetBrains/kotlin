/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.editor;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.testFramework.EditorTestUtil;
import kotlin.Unit;
import org.jetbrains.kotlin.formatter.FormatSettingsUtil;
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCaseKt;
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightTestCase;
import org.jetbrains.kotlin.test.KotlinTestUtils;
import org.junit.internal.runners.JUnit38ClassRunner;
import org.junit.runner.RunWith;

import java.io.File;

import static org.jetbrains.kotlin.idea.test.TestUtilsKt.IDEA_TEST_DATA_DIR;

@SuppressWarnings("deprecation")
@RunWith(JUnit38ClassRunner.class)
public class KotlinCommenterTest extends KotlinLightCodeInsightTestCase {
    private static final File BASE_DIR = new File(IDEA_TEST_DATA_DIR, "editor/commenter");

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

        KotlinLightCodeInsightFixtureTestCaseKt.configureCodeStyleAndRun(
                getProject(),
                settings -> {
                    FormatSettingsUtil.createConfigurator(getFile().getText(), settings).configureSettings();
                    return Unit.INSTANCE;
                },
                () -> {
                    executeAction("CommentByLineComment");
                    return Unit.INSTANCE;
                }
        );

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
        return new File(BASE_DIR, name);
    }

    private static String loadFile(String name) throws Exception {
        File file = getTestFile(name);
        return FileUtil.loadFile(file, true);
    }
}
