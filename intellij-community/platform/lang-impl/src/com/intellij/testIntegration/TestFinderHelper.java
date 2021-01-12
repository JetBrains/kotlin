// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.testIntegration;

import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNamedElement;
import com.intellij.util.text.NameUtilCore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public final class TestFinderHelper {
  public static PsiElement findSourceElement(@NotNull final PsiElement from) {
    for (TestFinder each : getFinders()) {
      PsiElement result = each.findSourceElement(from);
      if (result != null) return result;
    }
    return null;
  }

  public static Collection<PsiElement> findTestsForClass(@NotNull final PsiElement element) {
    Collection<PsiElement> result = new LinkedHashSet<>();
    for (TestFinder each : getFinders()) {
      result.addAll(each.findTestsForClass(element));
    }
    return result;
  }

  public static Collection<PsiElement> findClassesForTest(@NotNull final PsiElement element) {
    Collection<PsiElement> result = new LinkedHashSet<>();
    for (TestFinder each : getFinders()) {
      result.addAll(each.findClassesForTest(element));
    }
    return result;
  }

  public static boolean isTest(PsiElement element) {
    if (element == null) return false;
    for (TestFinder each : getFinders()) {
      if (each.isTest(element)) return true;
    }
    return false;
  }

  public static List<TestFinder> getFinders() {
    return TestFinder.EP_NAME.getExtensionList();
  }

  public static Integer calcTestNameProximity(final String className, final String testName) {
    int posProximity = testName.indexOf(className);
    int sizeProximity = testName.length() - className.length();

    return posProximity + sizeProximity;
  }

  public static List<PsiElement> getSortedElements(final List<? extends Pair<? extends PsiNamedElement, Integer>> elementsWithWeights,
                                                   final boolean weightsAscending) {
    return getSortedElements(elementsWithWeights, weightsAscending, null);
  }

  public static List<PsiElement> getSortedElements(final List<? extends Pair<? extends PsiNamedElement, Integer>> elementsWithWeights,
                                                   final boolean weightsAscending,
                                                   @Nullable final Comparator<? super PsiElement> sameNameComparator) {
    elementsWithWeights.sort((o1, o2) -> {
      int result = weightsAscending ? o1.second.compareTo(o2.second) : o2.second.compareTo(o1.second);
      if (result == 0) result = Comparing.compare(o1.first.getName(), o2.first.getName());
      if (result == 0 && sameNameComparator != null) result = sameNameComparator.compare(o1.first, o2.first);

      return result;
    });

    final List<PsiElement> result = new ArrayList<>(elementsWithWeights.size());
    for (Pair<? extends PsiNamedElement, Integer> each : elementsWithWeights) {
      result.add(each.first);
    }

    return result;
  }

  public static List<Pair<String, Integer>> collectPossibleClassNamesWithWeights(String testName) {
    String[] words = NameUtilCore.splitNameIntoWords(testName);
    List<Pair<String, Integer>> result = new ArrayList<>();

    for (int from = 0; from < words.length; from++) {
      for (int to = from; to < words.length; to++) {
        result.add(new Pair<>(StringUtil.join(words, from, to + 1, ""),
                              words.length - from + to));
      }
    }

    return result;
  }
}
