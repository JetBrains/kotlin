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

package org.jetbrains.kotlin.idea.navigation;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.intellij.codeInsight.navigation.GotoImplementationHandler;
import com.intellij.codeInsight.navigation.GotoTargetHandler;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.UsefulTestCase;
import com.intellij.util.PathUtil;
import com.intellij.util.containers.MultiMap;
import junit.framework.TestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.test.InTextDirectivesUtils;
import org.jetbrains.kotlin.test.ReferenceUtils;
import org.junit.Assert;

import java.util.*;

public final class NavigationTestUtils {
    private NavigationTestUtils() {
    }

    public static GotoTargetHandler.GotoData invokeGotoImplementations(Editor editor, PsiFile psiFile) {
        return new GotoImplementationHandler().getSourceAndTargetElements(editor, psiFile);
    }

    public static void assertGotoDataMatching(Editor editor, GotoTargetHandler.GotoData gotoData) {
        // Get expected references from the tested document
        List<String> expectedReferences = InTextDirectivesUtils.findListWithPrefixes(editor.getDocument().getText(), "// REF:");
        for (int i = 0; i < expectedReferences.size(); i++) {
            String expectedText = expectedReferences.get(i);
            expectedText = expectedText.replace("\\n", "\n");
            if (!expectedText.startsWith("<")) {
                expectedText = PathUtil.toSystemDependentName(expectedText).replace("//", "/");
            }
            expectedReferences.set(i, expectedText);
        }

        Collections.sort(expectedReferences);

        if (gotoData != null) {
            // Transform given reference result to strings
            List<String> psiElements = Lists.transform(Arrays.asList(gotoData.targets), new Function<PsiElement, String>() {
                @Override
                public String apply(@Nullable PsiElement element) {
                    Assert.assertNotNull(element);
                    return ReferenceUtils.renderAsGotoImplementation(element);
                }
            });

            // Compare
            UsefulTestCase.assertOrderedEquals(Ordering.natural().sortedCopy(psiElements), expectedReferences);
        }
        else {
            UsefulTestCase.assertOrderedEquals(Collections.emptyList(), expectedReferences);
        }
    }

    public static String getNavigateElementsText(Project project, Collection<? extends PsiElement> navigableElements) {
        MultiMap<PsiFile, Pair<Integer, Integer>> filesToNumbersAndOffsets = new MultiMap<PsiFile, Pair<Integer, Integer>>();
        int refNumber = 1;
        for (PsiElement navigationElement : navigableElements) {
            Pair<Integer, Integer> numberAndOffset = new Pair<Integer, Integer>(refNumber++, navigationElement.getTextOffset());
            filesToNumbersAndOffsets.putValue(navigationElement.getContainingFile(), numberAndOffset);
        }

        if (filesToNumbersAndOffsets.isEmpty()) {
            return "<no references>";
        }

        List<PsiFile> files = new ArrayList<PsiFile>(filesToNumbersAndOffsets.keySet());
        Collections.sort(files, new Comparator<PsiFile>() {
            @Override
            public int compare(@NotNull PsiFile f1, @NotNull PsiFile f2) {
                return f1.getName().compareTo(f2.getName());
            }
        });

        StringBuilder result = new StringBuilder();
        for (PsiFile file : files) {
            List<Pair<Integer, Integer>> numbersAndOffsets = new ArrayList<Pair<Integer, Integer>>(filesToNumbersAndOffsets.get(file));

            Collections.sort(numbersAndOffsets, Collections.reverseOrder(new Comparator<Pair<Integer, Integer>>() {
                @Override
                public int compare(Pair<Integer, Integer> t1, Pair<Integer, Integer> t2) {
                    int offsets = t1.second.compareTo(t2.second);
                    return offsets == 0 ? t1.first.compareTo(t2.first) : offsets;
                }
            }));

            Document document = PsiDocumentManager.getInstance(project).getDocument(file);
            TestCase.assertNotNull(document);
            StringBuilder resultForFile = new StringBuilder(document.getText());
            for (Pair<Integer, Integer> numberOffset : numbersAndOffsets) {
                resultForFile.insert(numberOffset.second, String.format("<%d>", numberOffset.first));
            }

            int minLine = Integer.MAX_VALUE;
            int maxLine = Integer.MIN_VALUE;
            for (Pair<Integer, Integer> numberOffset : numbersAndOffsets) {
                int lineNumber = document.getLineNumber(numberOffset.second);
                minLine = Math.min(minLine, lineNumber);
                maxLine = Math.max(maxLine, lineNumber);
            }

            Document annotated = EditorFactory.getInstance().createDocument(resultForFile);
            String filePart = annotated.getText().substring(annotated.getLineStartOffset(minLine),
                                                             annotated.getLineEndOffset(maxLine));
            result.append(" ").append(file.getName()).append("\n");
            result.append(filePart).append("\n");
        }
        return result.toString();
    }
}
