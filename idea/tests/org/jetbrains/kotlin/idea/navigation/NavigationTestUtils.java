/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.navigation;

import com.google.common.collect.Ordering;
import com.intellij.codeInsight.navigation.GotoTargetHandler;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.testFramework.UsefulTestCase;
import com.intellij.testFramework.fixtures.CodeInsightTestUtil;
import com.intellij.util.PathUtil;
import com.intellij.util.containers.MultiMap;
import junit.framework.TestCase;
import kotlin.collections.ArraysKt;
import org.jetbrains.kotlin.asJava.LightClassUtilsKt;
import org.jetbrains.kotlin.test.InTextDirectivesUtils;
import org.jetbrains.kotlin.test.util.ReferenceUtils;
import org.junit.Assert;

import java.util.*;
import java.util.stream.Collectors;

public final class NavigationTestUtils {
    private NavigationTestUtils() {
    }

    public static GotoTargetHandler.GotoData invokeGotoImplementations(Editor editor, PsiFile psiFile) {
        return CodeInsightTestUtil.gotoImplementation(editor, psiFile);
    }

    public static void assertGotoDataMatching(Editor editor, GotoTargetHandler.GotoData gotoData) {
        assertGotoDataMatching(editor, gotoData, false);
    }

    public static void assertGotoDataMatching(Editor editor, GotoTargetHandler.GotoData gotoData, boolean renderModule) {
        String documentText = editor.getDocument().getText();
        // Get expected references from the tested document
        List<String> expectedReferences = InTextDirectivesUtils.findListWithPrefixes(documentText, "// REF:");
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
            List<PsiElement> targets;
            if (InTextDirectivesUtils.isDirectiveDefined(documentText, "// DISTINCT_REF")) {
                targets = ArraysKt.distinctBy(gotoData.targets, LightClassUtilsKt::getUnwrapped);
            }
            else {
                targets = Arrays.asList(gotoData.targets);
            }
            // Transform given reference result to strings
            List<String> psiElements = targets.stream().map(element -> {
                Assert.assertNotNull(element);
                return ReferenceUtils.renderAsGotoImplementation(element, renderModule);
            }).collect(Collectors.toList());

            // Compare
            UsefulTestCase.assertOrderedEquals(Ordering.natural().sortedCopy(psiElements), expectedReferences);
        }
        else {
            UsefulTestCase.assertOrderedEquals(Collections.emptyList(), expectedReferences);
        }
    }

    public static String getNavigateElementsText(Project project, Collection<? extends PsiElement> navigableElements) {
        MultiMap<PsiFile, Pair<Integer, Integer>> filesToNumbersAndOffsets = new MultiMap<>();
        int refNumber = 1;
        for (PsiElement navigationElement : navigableElements) {
            Pair<Integer, Integer> numberAndOffset = new Pair<>(refNumber++, navigationElement.getTextOffset());
            filesToNumbersAndOffsets.putValue(navigationElement.getContainingFile(), numberAndOffset);
        }

        if (filesToNumbersAndOffsets.isEmpty()) {
            return "<no references>";
        }

        List<PsiFile> files = new ArrayList<>(filesToNumbersAndOffsets.keySet());
        files.sort(Comparator.comparing(PsiFileSystemItem::getName));

        StringBuilder result = new StringBuilder();
        for (PsiFile file : files) {
            List<Pair<Integer, Integer>> numbersAndOffsets = new ArrayList<>(filesToNumbersAndOffsets.get(file));

            numbersAndOffsets.sort(Collections.reverseOrder(
                    Comparator.comparingInt((Pair<Integer, Integer> t) -> t.second).thenComparingInt(t -> t.first)));

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
