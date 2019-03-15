/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.highlighter;

import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerImpl;
import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.rt.execution.junit.FileComparisonFailure;
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase;
import org.jetbrains.kotlin.test.InTextDirectivesUtils;
import org.jetbrains.kotlin.test.TagsTestDataUtil;

import java.io.File;
import java.util.List;

public abstract class AbstractHighlightingTest extends KotlinLightCodeInsightFixtureTestCase {

    protected void doTest(String filePath) throws Exception {
        String fileText = FileUtil.loadFile(new File(filePath), true);
        boolean checkInfos = !InTextDirectivesUtils.isDirectiveDefined(fileText, "// NO_CHECK_INFOS");
        boolean checkWeakWarnings = !InTextDirectivesUtils.isDirectiveDefined(fileText, "// NO_CHECK_WEAK_WARNINGS");
        boolean checkWarnings = !InTextDirectivesUtils.isDirectiveDefined(fileText, "// NO_CHECK_WARNINGS");
        boolean expectedDuplicatedHighlighting = InTextDirectivesUtils.isDirectiveDefined(fileText, "// EXPECTED_DUPLICATED_HIGHLIGHTING");

        myFixture.configureByFile(filePath);

        withExpectedDuplicatedHighlighting(expectedDuplicatedHighlighting, () -> {
            try {
                myFixture.checkHighlighting(checkWarnings, checkInfos, checkWeakWarnings);
            }
            catch (FileComparisonFailure e) {
                List<HighlightInfo> highlights =
                        DaemonCodeAnalyzerImpl.getHighlights(myFixture.getDocument(myFixture.getFile()), null, myFixture.getProject());
                String text = myFixture.getFile().getText();

                System.out.println(TagsTestDataUtil.insertInfoTags(highlights, text));
                throw e;
            }
        });
    }

    private static void withExpectedDuplicatedHighlighting(boolean expectedDuplicatedHighlighting, Runnable runnable) {
        if (!expectedDuplicatedHighlighting) {
            runnable.run();
            return;
        }

        DuplicateHighlightingKt.expectedDuplicatedHighlighting(runnable);
    }
}
