// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.find.findUsages;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.usageView.UsageInfo;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.Processor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * @author peter
 * @see FindUsagesHandlerFactory
 */
public abstract class FindUsagesHandler extends FindUsagesHandlerBase implements FindUsagesHandlerUi {
  // return this handler if you want to cancel the search
  @NotNull
  public static final FindUsagesHandler NULL_HANDLER = new NullFindUsagesHandler();

  protected FindUsagesHandler(@NotNull PsiElement psiElement) {
    super(psiElement);
  }

  @Override
  @NotNull
  public AbstractFindUsagesDialog getFindUsagesDialog(boolean isSingleFile, boolean toShowInNewTab, boolean mustOpenInNewTab) {
    return createDefaultFindUsagesDialog(isSingleFile, toShowInNewTab, mustOpenInNewTab, this);
  }

  @NotNull
  public static AbstractFindUsagesDialog createDefaultFindUsagesDialog(boolean isSingleFile,
                                                                       boolean toShowInNewTab,
                                                                       boolean mustOpenInNewTab,
                                                                       @NotNull FindUsagesHandlerBase handler) {
    @SuppressWarnings("deprecation") DataContext ctx = DataManager.getInstance().getDataContext();
    return new CommonFindUsagesDialog(handler.getPsiElement(), handler.getProject(), handler.getFindUsagesOptions(ctx), toShowInNewTab, mustOpenInNewTab, isSingleFile,
                                      handler);
  }

  @Override
  @Nullable
  public String getHelpId() {
    return FindUsagesManager.getHelpID(myPsiElement);
  }

  private static class NullFindUsagesHandler extends FindUsagesHandler {
    private NullFindUsagesHandler() {
      super(PsiUtilCore.NULL_PSI_ELEMENT);
    }

    @NotNull
    @Override
    public AbstractFindUsagesDialog getFindUsagesDialog(boolean isSingleFile, boolean toShowInNewTab, boolean mustOpenInNewTab) {
      throw new IncorrectOperationException();
    }

    @Override
    public PsiElement @NotNull [] getPrimaryElements() {
      throw new IncorrectOperationException();
    }

    @Override
    public PsiElement @NotNull [] getSecondaryElements() {
      throw new IncorrectOperationException();
    }

    @Nullable
    @Override
    public String getHelpId() {
      throw new IncorrectOperationException();
    }

    @NotNull
    @Override
    public FindUsagesOptions getFindUsagesOptions() {
      throw new IncorrectOperationException();
    }

    @NotNull
    @Override
    public FindUsagesOptions getFindUsagesOptions(@Nullable DataContext dataContext) {
      throw new IncorrectOperationException();
    }

    @Override
    public boolean processElementUsages(@NotNull PsiElement element,
                                        @NotNull Processor<? super UsageInfo> processor,
                                        @NotNull FindUsagesOptions options) {
      throw new IncorrectOperationException();
    }

    @Override
    public boolean processUsagesInText(@NotNull PsiElement element,
                                       @NotNull Processor<? super UsageInfo> processor,
                                       @NotNull GlobalSearchScope searchScope) {
      throw new IncorrectOperationException();
    }

    @Nullable
    @Override
    protected Collection<String> getStringsToSearch(@NotNull PsiElement element) {
      throw new IncorrectOperationException();
    }

    @Override
    protected boolean isSearchForTextOccurrencesAvailable(@NotNull PsiElement psiElement, boolean isSingleFile) {
      throw new IncorrectOperationException();
    }

    @NotNull
    @Override
    public Collection<PsiReference> findReferencesToHighlight(@NotNull PsiElement target, @NotNull SearchScope searchScope) {
      throw new IncorrectOperationException();
    }
  }
}
