// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.refactoring.util;

import com.intellij.find.FindManager;
import com.intellij.find.findUsages.FindUsagesHandler;
import com.intellij.find.findUsages.FindUsagesManager;
import com.intellij.find.findUsages.FindUsagesUtil;
import com.intellij.find.impl.FindManagerImpl;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.usageView.UsageInfo;
import com.intellij.usageView.UsageInfoFactory;
import com.intellij.util.PairProcessor;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public final class TextOccurrencesUtil {
  private TextOccurrencesUtil() {
  }

  /** @deprecated Use {@link TextOccurrencesUtil#addTextOccurrences} */
  @Deprecated
  public static void addTextOccurences(@NotNull PsiElement element,
                                       @NotNull String stringToSearch,
                                       @NotNull GlobalSearchScope searchScope,
                                       @NotNull final Collection<? super UsageInfo> results,
                                       @NotNull final UsageInfoFactory factory) {
    addTextOccurrences(element, stringToSearch, searchScope, results, factory);
  }

  public static void addTextOccurrences(@NotNull PsiElement element,
                                        @NotNull String stringToSearch,
                                        @NotNull GlobalSearchScope searchScope,
                                        @NotNull Collection<? super UsageInfo> results,
                                        @NotNull UsageInfoFactory factory) {
    TextOccurrencesUtilBase.addTextOccurrences(element, stringToSearch, searchScope, results, factory);
  }

    /** @deprecated Use {@link TextOccurrencesUtil#processUsagesInStringsAndComments(
     * PsiElement, SearchScope, String, boolean, PairProcessor)} */
  @Deprecated
  public static boolean processUsagesInStringsAndComments(@NotNull PsiElement element,
                                                          @NotNull String stringToSearch,
                                                          boolean ignoreReferences,
                                                          @NotNull PairProcessor<? super PsiElement, ? super TextRange> processor) {
    return processUsagesInStringsAndComments(element, GlobalSearchScope.projectScope(element.getProject()),
                                             stringToSearch, ignoreReferences, processor);
  }

  public static boolean processUsagesInStringsAndComments(@NotNull PsiElement element,
                                                          @NotNull SearchScope searchScope,
                                                          @NotNull String stringToSearch,
                                                          boolean ignoreReferences,
                                                          @NotNull PairProcessor<? super PsiElement, ? super TextRange> processor) {
    return TextOccurrencesUtilBase.processUsagesInStringsAndComments(element, searchScope, stringToSearch, ignoreReferences, processor);
  }

  /** @deprecated Use {@link TextOccurrencesUtil#addUsagesInStringsAndComments(
   * PsiElement, SearchScope, String, Collection, UsageInfoFactory)} */
  @Deprecated
  public static void addUsagesInStringsAndComments(@NotNull PsiElement element,
                                                   @NotNull String stringToSearch,
                                                   @NotNull Collection<? super UsageInfo> results,
                                                   @NotNull UsageInfoFactory factory) {
    addUsagesInStringsAndComments(element, GlobalSearchScope.projectScope(element.getProject()), stringToSearch, results, factory);
  }

  public static void addUsagesInStringsAndComments(@NotNull PsiElement element,
                                                   @NotNull SearchScope searchScope,
                                                   @NotNull String stringToSearch,
                                                   @NotNull Collection<? super UsageInfo> results,
                                                   @NotNull UsageInfoFactory factory) {
    TextOccurrencesUtilBase.addUsagesInStringsAndComments(element, searchScope, stringToSearch, results, factory);
  }

  public static boolean isSearchTextOccurrencesEnabled(@NotNull PsiElement element) {
    FindUsagesManager findUsagesManager = ((FindManagerImpl)FindManager.getInstance(element.getProject())).getFindUsagesManager();
    FindUsagesHandler handler = findUsagesManager.getFindUsagesHandler(element, true);
    return FindUsagesUtil.isSearchForTextOccurrencesAvailable(element, false, handler);
  }

  /** @deprecated Use {@link TextOccurrencesUtil#findNonCodeUsages(
   * PsiElement, SearchScope, String, boolean, boolean, String, Collection)} */
  @Deprecated
  public static void findNonCodeUsages(PsiElement element,
                                       String stringToSearch,
                                       boolean searchInStringsAndComments,
                                       boolean searchInNonJavaFiles,
                                       String newQName,
                                       Collection<? super UsageInfo> results) {
    findNonCodeUsages(element, GlobalSearchScope.projectScope(element.getProject()),
                      stringToSearch, searchInStringsAndComments, searchInNonJavaFiles, newQName, results);
  }

  public static void findNonCodeUsages(@NotNull PsiElement element,
                                       @NotNull SearchScope searchScope,
                                       String stringToSearch,
                                       boolean searchInStringsAndComments,
                                       boolean searchInNonJavaFiles,
                                       String newQName,
                                       Collection<? super UsageInfo> results) {
    if (searchInStringsAndComments || searchInNonJavaFiles) {
      UsageInfoFactory factory = createUsageInfoFactory(element, newQName);

      if (searchInStringsAndComments) {
        addUsagesInStringsAndComments(element, searchScope, stringToSearch, results, factory);
      }

      if (searchInNonJavaFiles && searchScope instanceof GlobalSearchScope) {
        addTextOccurrences(element, stringToSearch, (GlobalSearchScope)searchScope, results, factory);
      }
    }
  }

  private static UsageInfoFactory createUsageInfoFactory(final PsiElement element,
                                                        final String newQName) {
    return (usage, startOffset, endOffset) -> {
      int start = usage.getTextRange().getStartOffset();
      return NonCodeUsageInfo.create(usage.getContainingFile(), start + startOffset, start + endOffset, element,
                                     newQName);
    };
  }
}
