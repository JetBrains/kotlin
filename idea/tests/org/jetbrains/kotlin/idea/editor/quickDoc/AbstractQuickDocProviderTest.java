/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.editor.quickDoc;

import com.intellij.codeInsight.documentation.DocumentationManager;
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
import java.util.stream.Collectors;

public abstract class AbstractQuickDocProviderTest extends KotlinLightCodeInsightFixtureTestCase {
    public void doTest(@NotNull String path) throws Exception {
        IdeaTestUtilsKt.configureWithExtraFile(myFixture, path, "_Data");

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

            String cleanedInfo = info == null ? "" : StringUtil.join(
                    StringUtil.split(info, "\n", false)
                            .stream()
                            .map(s -> StringUtil.isEmptyOrSpaces(s) ? "\n" : s)
                            .collect(Collectors.toList()),
                    "");

            if (expectedInfo.endsWith("...\n")) {
                if (!cleanedInfo.startsWith(StringUtil.trimEnd(expectedInfo, "...\n"))) {
                    wrapToFileComparisonFailure(cleanedInfo, path, textData);
                }
            }
            else if (!expectedInfo.equals(cleanedInfo)) {
                wrapToFileComparisonFailure(cleanedInfo, path, textData);
            }
        }
    }

    public static void wrapToFileComparisonFailure(String info, String filePath, String fileData) {
        List<String> infoLines = StringUtil.split(info, "\n", false);
        StringBuilder infoBuilder = new StringBuilder();
        for (String line : infoLines) {
            infoBuilder.append("//INFO: ").append(line);
        }

        String correctedFileText = fileData.replaceAll("//\\s?INFO:\\s?.*\n?", "") + infoBuilder.toString();
        throw new FileComparisonFailure("Unexpected info", fileData, correctedFileText, new File(filePath).getAbsolutePath());
    }


    @NotNull
    @Override
    protected LightProjectDescriptor getProjectDescriptor() {
        return ProjectDescriptorWithStdlibSources.INSTANCE;
    }
}
