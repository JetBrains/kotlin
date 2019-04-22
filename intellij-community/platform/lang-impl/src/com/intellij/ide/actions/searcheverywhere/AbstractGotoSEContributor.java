// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions.searcheverywhere;

import com.intellij.codeInsight.navigation.NavigationUtil;
import com.intellij.ide.actions.QualifiedNameProviderUtil;
import com.intellij.ide.actions.SearchEverywherePsiRenderer;
import com.intellij.ide.util.EditSourceUtil;
import com.intellij.ide.util.gotoByName.ChooseByNamePopup;
import com.intellij.ide.util.gotoByName.FilteringGotoByModel;
import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.util.ProgressIndicatorUtils;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.Navigatable;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.util.Processor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class AbstractGotoSEContributor<Filter> implements SearchEverywhereContributor<Object, Filter> {

  protected static final Pattern patternToDetectLinesAndColumns = Pattern.compile("(.+?)" + // name, non-greedy matching
                                                                                "(?::|@|,| |#|#L|\\?l=| on line | at line |:?\\(|:?\\[)" + // separator
                                                                                "(\\d+)?(?:\\W(\\d+)?)?" + // line + column
                                                                                "[)\\]]?" // possible closing paren/brace
  );
  protected static final Pattern patternToDetectAnonymousClasses = Pattern.compile("([\\.\\w]+)((\\$[\\d]+)*(\\$)?)");
  protected static final Pattern patternToDetectMembers = Pattern.compile("(.+)(#)(.*)");
  protected static final Pattern patternToDetectSignatures = Pattern.compile("(.+#.*)\\(.*\\)");

  //space character in the end of pattern forces full matches search
  private static final String fullMatchSearchSuffix = " ";

  @Nullable protected final Project myProject;
  @Nullable protected final PsiElement psiContext;

  protected AbstractGotoSEContributor(@Nullable Project project, @Nullable PsiElement context) {
    myProject = project;
    psiContext = context;
  }

  @NotNull
  @Override
  public String getSearchProviderId() {
    return getClass().getSimpleName();
  }

  @Override
  public boolean isShownInSeparateTab() {
    return true;
  }

  private static final Logger LOG = Logger.getInstance(AbstractGotoSEContributor.class);

  @Override
  public void fetchElements(@NotNull String pattern,
                            boolean everywhere,
                            @Nullable SearchEverywhereContributorFilter<Filter> filter,
                            @NotNull ProgressIndicator progressIndicator,
                            @NotNull Processor<? super Object> consumer) {
    if (myProject == null) return; //nothing to search
    if (!isEmptyPatternSupported() && pattern.isEmpty()) return;

    ProgressIndicatorUtils.yieldToPendingWriteActions();
    ProgressIndicatorUtils.runInReadActionWithWriteActionPriority(() -> {
      if (!isDumbModeSupported() && DumbService.getInstance(myProject).isDumb()) return;

      FilteringGotoByModel<Filter> model = createModel(myProject);
      if (filter != null) {
        model.setFilterItems(filter.getSelectedElements());
      }

      if (progressIndicator.isCanceled()) return;

      PsiElement context = psiContext != null && psiContext.isValid() ? psiContext : null;
      ChooseByNamePopup popup = ChooseByNamePopup.createPopup(myProject, model, context);
      try {
        popup.getProvider().filterElements(popup, pattern, everywhere, progressIndicator, element -> {
          if (progressIndicator.isCanceled()) return false;
          if (element == null) {
            LOG.error("Null returned from " + model + " in " + this);
            return true;
          }
          return consumer.process(element);
        });
      }
      finally {
        Disposer.dispose(popup);
      }
    }, progressIndicator);
  }

  @NotNull
  protected abstract FilteringGotoByModel<Filter> createModel(@NotNull Project project);

  @NotNull
  @Override
  public String filterControlSymbols(@NotNull String pattern) {
    if (StringUtil.containsAnyChar(pattern, ":,;@[( #") ||
        pattern.contains(" line ") ||
        pattern.contains("?l=")) { // quick test if reg exp should be used
      return applyPatternFilter(pattern, patternToDetectLinesAndColumns);
    }

    return pattern;
  }

  protected static String applyPatternFilter(String str, Pattern regex) {
    Matcher matcher = regex.matcher(str);
    if (matcher.matches()) {
      return matcher.group(1);
    }

    return str;
  }

  @Override
  public boolean showInFindResults() {
    return true;
  }

  @Override
  public boolean processSelectedItem(@NotNull Object selected, int modifiers, @NotNull String searchText) {
    if (selected instanceof PsiElement) {
      if (!((PsiElement)selected).isValid()) {
        LOG.warn("Cannot navigate to invalid PsiElement");
        return true;
      }

      PsiElement psiElement = preparePsi((PsiElement)selected, modifiers, searchText);
      Navigatable extNavigatable = createExtendedNavigatable(psiElement, searchText, modifiers);
      if (extNavigatable != null && extNavigatable.canNavigate()) {
        extNavigatable.navigate(true);
        return true;
      }

      NavigationUtil.activateFileWithPsiElement(psiElement, openInCurrentWindow(modifiers));
    }
    else {
      EditSourceUtil.navigate(((NavigationItem)selected), true, openInCurrentWindow(modifiers));
    }

    return true;
  }

  @Override
  public Object getDataForItem(@NotNull Object element, @NotNull String dataId) {
    if (CommonDataKeys.PSI_ELEMENT.is(dataId)) {
      if (element instanceof PsiElement) {
        return element;
      }
      if (element instanceof DataProvider) {
        return ((DataProvider)element).getData(dataId);
      }
    }

    if (SearchEverywhereDataKeys.ITEM_STRING_DESCRIPTION.is(dataId) && element instanceof PsiElement) {
      return QualifiedNameProviderUtil.getQualifiedName((PsiElement)element);
    }

    return null;
  }

  @Override
  public boolean isMultiSelectionSupported() {
    return true;
  }

  @NotNull
  @Override
  public ListCellRenderer<Object> getElementsRenderer() {
    return new SERenderer();
  }

  @Override
  public int getElementPriority(@NotNull Object element, @NotNull String searchPattern) {
    return 50;
  }

  @Nullable
  protected Navigatable createExtendedNavigatable(PsiElement psi, String searchText, int modifiers) {
    VirtualFile file = PsiUtilCore.getVirtualFile(psi);
    Pair<Integer, Integer> position = getLineAndColumn(searchText);
    boolean positionSpecified = position.first >= 0 || position.second >= 0;
    if (file != null && positionSpecified) {
      OpenFileDescriptor descriptor = new OpenFileDescriptor(psi.getProject(), file, position.first, position.second);
      return descriptor.setUseCurrentWindow(openInCurrentWindow(modifiers));
    }

    return null;
  }

  protected PsiElement preparePsi(PsiElement psiElement, int modifiers, String searchText) {
    return psiElement.getNavigationElement();
  }

  protected static Pair<Integer, Integer> getLineAndColumn(String text) {
    int line = getLineAndColumnRegexpGroup(text, 2);
    int column = getLineAndColumnRegexpGroup(text, 3);

    if (line == -1 && column != -1) {
      line = 0;
    }

    return new Pair<>(line, column);
  }

  private static int getLineAndColumnRegexpGroup(String text, int groupNumber) {
    final Matcher matcher = patternToDetectLinesAndColumns.matcher(text);
    if (matcher.matches()) {
      try {
        if (groupNumber <= matcher.groupCount()) {
          final String group = matcher.group(groupNumber);
          if (group != null) return Integer.parseInt(group) - 1;
        }
      }
      catch (NumberFormatException ignored) {
      }
    }

    return -1;
  }

  protected static boolean openInCurrentWindow(int modifiers) {
    return (modifiers & InputEvent.SHIFT_MASK) == 0;
  }

  protected static class SERenderer extends SearchEverywherePsiRenderer {
    @Override
    public String getElementText(PsiElement element) {
      if (element instanceof NavigationItem) {
        return Optional.ofNullable(((NavigationItem)element).getPresentation())
          .map(presentation -> presentation.getPresentableText())
          .orElse(super.getElementText(element));
      }
      return super.getElementText(element);
    }
  }
}
