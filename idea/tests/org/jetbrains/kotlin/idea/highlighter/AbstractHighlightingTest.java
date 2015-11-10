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

import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerImpl;
import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.rt.execution.junit.FileComparisonFailure;
import com.intellij.testFramework.LightProjectDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase;
import org.jetbrains.kotlin.idea.test.KotlinLightProjectDescriptor;
import org.jetbrains.kotlin.test.InTextDirectivesUtils;
import org.jetbrains.kotlin.test.TagsTestDataUtil;

import java.io.File;
import java.util.List;

public abstract class AbstractHighlightingTest extends KotlinLightCodeInsightFixtureTestCase {
    @NotNull
    @Override
    protected LightProjectDescriptor getProjectDescriptor() {
        return KotlinLightProjectDescriptor.INSTANCE;
    }

    protected void doTest(String filePath) throws Exception {
        String fileText = FileUtil.loadFile(new File(filePath), true);
        boolean checkInfos = !InTextDirectivesUtils.isDirectiveDefined(fileText, "// NO_CHECK_INFOS");
        boolean checkWeakWarnings = !InTextDirectivesUtils.isDirectiveDefined(fileText, "// NO_CHECK_WEAK_WARNINGS");
        boolean checkWarnings = !InTextDirectivesUtils.isDirectiveDefined(fileText, "// NO_CHECK_WARNINGS");

        myFixture.configureByFile(filePath);

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
    }

    @NotNull
    @Override
    protected String getTestDataPath() {
        return "";
    }
}
