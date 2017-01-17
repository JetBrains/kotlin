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

package org.jetbrains.kotlin.idea.editor.quickDoc;

import com.intellij.codeInsight.documentation.DocumentationComponent;
import com.intellij.codeInsight.documentation.DocumentationManager;
import com.intellij.codeInsight.navigation.CtrlMouseHandler;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.rt.execution.junit.FileComparisonFailure;
import com.intellij.testFramework.LightProjectDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.completion.test.IdeaTestUtilsKt;
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase;
import org.jetbrains.kotlin.idea.test.ProjectDescriptorWithStdlibSources;
import org.jetbrains.kotlin.test.InTextDirectivesUtils;

import java.io.File;
import java.util.List;

public abstract class AbstractQuickDocProviderTest extends KotlinLightCodeInsightFixtureTestCase {
    public void doTest(@NotNull String path) throws Exception {
        IdeaTestUtilsKt.configureWithExtraFileAbs(myFixture, path, "_Data");

        PsiElement element = myFixture.getFile().findElementAt(myFixture.getEditor().getCaretModel().getOffset());
        assertNotNull("Can't find element at caret in file: " + path, element);

        DocumentationManager documentationManager = DocumentationManager.getInstance(myFixture.getProject());
        PsiElement targetElement = documentationManager.findTargetElement(myFixture.getEditor(), myFixture.getFile());
        PsiElement originalElement = DocumentationManager.getOriginalElement(targetElement);

        String info = DocumentationManager.getProviderFromElement(targetElement).generateDoc(targetElement, originalElement);
        if (info != null) {
            info = StringUtil.convertLineSeparators(info);
        }
        if (info != null && !info.endsWith("\n")) {
            info += "\n";
        }

        File testDataFile = new File(path);
        String textData = FileUtil.loadFile(testDataFile, true);
        List<String> directives = InTextDirectivesUtils.findLinesWithPrefixesRemoved(textData, false, "INFO:");

        if (directives.isEmpty()) {
            throw new FileComparisonFailure(
                    "'// INFO:' directive was expected",
                    textData,
                    textData + "\n\n//INFO: " + info,
                    testDataFile.getAbsolutePath());
        }
        else {
            StringBuilder expectedInfoBuilder = new StringBuilder();
            for (String directive : directives) {
                expectedInfoBuilder.append(directive).append("\n");
            }
            String expectedInfo = expectedInfoBuilder.toString();

            if (expectedInfo.endsWith("...\n")) {
                if (!info.startsWith(StringUtil.trimEnd(expectedInfo, "...\n"))) {
                    wrapToFileComparisonFailure(info, path, textData);
                }
            }
            else if (!expectedInfo.equals(info)) {
                wrapToFileComparisonFailure(info, path, textData);
            }
        }
    }

    public static void wrapToFileComparisonFailure(String info, String filePath, String fileData) {
        List<String> infoLines = StringUtil.split(info, "\n");
        StringBuilder infoBuilder = new StringBuilder();
        for (String line : infoLines) {
            infoBuilder.append("//INFO: ").append(line).append("\n");
        }

        String correctedFileText = fileData.replaceAll("//\\s?INFO: .*\n?", "") + infoBuilder.toString();
        throw new FileComparisonFailure("Unexpected info", fileData, correctedFileText, new File(filePath).getAbsolutePath());
    }


    @NotNull
    @Override
    protected LightProjectDescriptor getProjectDescriptor() {
        return ProjectDescriptorWithStdlibSources.INSTANCE;
    }
}
