/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.find.findUsages;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.application.ReadActionProcessor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.usageView.UsageInfo;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.Processor;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;

/**
 * @author peter
 * @see FindUsagesHandlerFactory
 */
public abstract class FindUsagesHandler {
  // return this handler if you want to cancel the search
  @NotNull
  public static final FindUsagesHandler NULL_HANDLER = new NullFindUsagesHandler();

  @NotNull
  private final PsiElement myPsiElement;

  protected FindUsagesHandler(@NotNull PsiElement psiElement) {
    myPsiElement = psiElement;
  }

  @NotNull
  public AbstractFindUsagesDialog getFindUsagesDialog(boolean isSingleFile, boolean toShowInNewTab, boolean mustOpenInNewTab) {
    @SuppressWarnings("deprecation") DataContext ctx = DataManager.getInstance().getDataContext();
    return new CommonFindUsagesDialog(myPsiElement, getProject(), getFindUsagesOptions(ctx), toShowInNewTab, mustOpenInNewTab, isSingleFile, this);
  }

  @NotNull
  public final PsiElement getPsiElement() {
    return myPsiElement;
  }

  @NotNull
  public final Project getProject() {
    return myPsiElement.getProject();
  }

  @NotNull
  public PsiElement[] getPrimaryElements() {
    return new PsiElement[]{myPsiElement};
  }

  @NotNull
  public PsiElement[] getSecondaryElements() {
    return PsiElement.EMPTY_ARRAY;
  }

  @Nullable
  protected String getHelpId() {
    return FindUsagesManager.getHelpID(myPsiElement);
  }

  @NotNull
  public static FindUsagesOptions createFindUsagesOptions(@NotNull Project project, @Nullable final DataContext dataContext) {
    FindUsagesOptions findUsagesOptions = new FindUsagesOptions(project, dataContext);
    findUsagesOptions.isUsages = true;
    findUsagesOptions.isSearchForTextOccurrences = true;
    return findUsagesOptions;
  }

  @NotNull
  public FindUsagesOptions getFindUsagesOptions() {
    return getFindUsagesOptions(null);
  }

  @NotNull
  public FindUsagesOptions getFindUsagesOptions(@Nullable final DataContext dataContext) {
    FindUsagesOptions options = createFindUsagesOptions(getProject(), dataContext);
    options.isSearchForTextOccurrences &= isSearchForTextOccurrencesAvailable(getPsiElement(), false);
    return options;
  }

  public boolean processElementUsages(@NotNull final PsiElement element,
                                      @NotNull final Processor<UsageInfo> processor,
                                      @NotNull final FindUsagesOptions options) {
    final ReadActionProcessor<PsiReference> refProcessor = new ReadActionProcessor<PsiReference>() {
      @Override
      public boolean processInReadAction(final PsiReference ref) {
        TextRange rangeInElement = ref.getRangeInElement();
        return processor.process(new UsageInfo(ref.getElement(), rangeInElement.getStartOffset(), rangeInElement.getEndOffset(), false));
      }
    };

    final SearchScope scope = options.searchScope;

    final boolean searchText = options.isSearchForTextOccurrences && scope instanceof GlobalSearchScope;

    if (options.isUsages) {
      boolean success =
        ReferencesSearch.search(new ReferencesSearch.SearchParameters(element, scope, false, options.fastTrack)).forEach(refProcessor);
      if (!success) return false;
    }

    if (searchText) {
      if (options.fastTrack != null) {
        options.fastTrack.searchCustom(consumer -> processUsagesInText(element, processor, (GlobalSearchScope)scope));
      }
      else {
        return processUsagesInText(element, processor, (GlobalSearchScope)scope);
      }
    }
    return true;
  }

  public boolean processUsagesInText(@NotNull final PsiElement element,
                                     @NotNull Processor<UsageInfo> processor,
                                     @NotNull GlobalSearchScope searchScope) {
    Collection<String> stringToSearch = ReadAction.compute(() -> getStringsToSearch(element));
    if (stringToSearch == null) return true;
    return FindUsagesHelper.processUsagesInText(element, stringToSearch, searchScope, processor);
  }

  @Nullable
  protected Collection<String> getStringsToSearch(@NotNull final PsiElement element) {
    if (element instanceof PsiNamedElement) {
      return ContainerUtil.createMaybeSingletonList(((PsiNamedElement)element).getName());
    }

    return Collections.singleton(element.getText());
  }

  protected boolean isSearchForTextOccurrencesAvailable(@NotNull PsiElement psiElement, boolean isSingleFile) {
    return isSearchForTextOccurencesAvailable(psiElement, isSingleFile);
  }

  /** @deprecated use/override {@link #isSearchForTextOccurrencesAvailable(PsiElement, boolean)} instead (to be removed in IDEA 18) */
  @Deprecated
  @SuppressWarnings("SpellCheckingInspection")
  protected boolean isSearchForTextOccurencesAvailable(@NotNull PsiElement psiElement, boolean isSingleFile) {
    return false;
  }

  @NotNull
  public Collection<PsiReference> findReferencesToHighlight(@NotNull PsiElement target, @NotNull SearchScope searchScope) {
    return ReferencesSearch.search(target, searchScope, false).findAll();
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

    @NotNull
    @Override
    public PsiElement[] getPrimaryElements() {
      throw new IncorrectOperationException();
    }

    @NotNull
    @Override
    public PsiElement[] getSecondaryElements() {
      throw new IncorrectOperationException();
    }

    @Nullable
    @Override
    protected String getHelpId() {
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
                                        @NotNull Processor<UsageInfo> processor,
                                        @NotNull FindUsagesOptions options) {
      throw new IncorrectOperationException();
    }

    @Override
    public boolean processUsagesInText(@NotNull PsiElement element,
                                       @NotNull Processor<UsageInfo> processor,
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
