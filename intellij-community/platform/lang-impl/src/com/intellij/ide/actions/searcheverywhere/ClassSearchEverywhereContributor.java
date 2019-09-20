// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions.searcheverywhere;

import com.intellij.codeInsight.navigation.NavigationUtil;
import com.intellij.ide.IdeBundle;
import com.intellij.ide.actions.GotoActionBase;
import com.intellij.ide.actions.GotoClassAction;
import com.intellij.ide.actions.GotoClassPresentationUpdater;
import com.intellij.ide.util.gotoByName.FilteringGotoByModel;
import com.intellij.ide.util.gotoByName.GotoClassModel2;
import com.intellij.ide.util.gotoByName.GotoClassSymbolConfiguration;
import com.intellij.lang.DependentLanguage;
import com.intellij.lang.Language;
import com.intellij.lang.LanguageUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.Navigatable;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.ui.IdeUICustomization;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author Konstantin Bulenkov
 */
public class ClassSearchEverywhereContributor extends AbstractGotoSEContributor {

  private static final Pattern ourPatternToDetectAnonymousClasses = Pattern.compile("([.\\w]+)((\\$[\\d]+)*(\\$)?)");
  private static final Pattern ourPatternToDetectMembers = Pattern.compile("(.+)(#)(.*)");

  private final PersistentSearchEverywhereContributorFilter<Language> myFilter;

  public ClassSearchEverywhereContributor(@Nullable Project project, @Nullable PsiElement context) {
    super(project, context);
    myFilter = project == null ? null : createLanguageFilter(project);
  }

  @NotNull
  @Override
  public String getGroupName() {
    return GotoClassPresentationUpdater.getTabTitle(true);
  }

  @NotNull
  @Override
  public String getFullGroupName() {
    String[] split = GotoClassPresentationUpdater.getActionTitle().split("/");
    return Arrays.stream(split)
      .map(StringUtil::pluralize)
      .collect(Collectors.joining("/"));
  }

  @NotNull
  public String includeNonProjectItemsText() {
    return IdeBundle.message("checkbox.include.non.project.classes", IdeUICustomization.getInstance().getProjectConceptName());
  }

  @Override
  public int getSortWeight() {
    return 100;
  }

  @NotNull
  @Override
  protected FilteringGotoByModel<Language> createModel(@NotNull Project project) {
    GotoClassModel2 model = new GotoClassModel2(project);
    if (myFilter != null) {
      model.setFilterItems(myFilter.getSelectedElements());
    }
    return model;
  }

  @NotNull
  @Override
  public List<AnAction> getActions(@NotNull Runnable onChanged) {
    return doGetActions(includeNonProjectItemsText(), myFilter, onChanged);
  }

  @NotNull
  @Override
  public String filterControlSymbols(@NotNull String pattern) {
    if (pattern.indexOf('#') != -1) {
      pattern = applyPatternFilter(pattern, ourPatternToDetectMembers);
    }

    if (pattern.indexOf('$') != -1) {
      pattern = applyPatternFilter(pattern, ourPatternToDetectAnonymousClasses);
    }

    return super.filterControlSymbols(pattern);
  }

  @Override
  public int getElementPriority(@NotNull Object element, @NotNull String searchPattern) {
    return super.getElementPriority(element, searchPattern) + 5;
  }

  @Override
  protected PsiElement preparePsi(PsiElement psiElement, int modifiers, String searchText) {
    String path = pathToAnonymousClass(searchText);
    if (path != null) {
      psiElement = GotoClassAction.getElement(psiElement, path);
    }
    return super.preparePsi(psiElement, modifiers, searchText);
  }

  @Nullable
  @Override
  protected Navigatable createExtendedNavigatable(PsiElement psi, String searchText, int modifiers) {
    Navigatable res = super.createExtendedNavigatable(psi, searchText, modifiers);
    if (res != null) {
      return res;
    }

    VirtualFile file = PsiUtilCore.getVirtualFile(psi);
    String memberName = getMemberName(searchText);
    if (file != null && memberName != null) {
      Navigatable delegate = GotoClassAction.findMember(memberName, searchText, psi, file);
      if (delegate != null) {
        return new Navigatable() {
          @Override
          public void navigate(boolean requestFocus) {
            NavigationUtil.activateFileWithPsiElement(psi, openInCurrentWindow(modifiers));
            delegate.navigate(true);

          }

          @Override
          public boolean canNavigate() {
            return delegate.canNavigate();
          }

          @Override
          public boolean canNavigateToSource() {
            return delegate.canNavigateToSource();
          }
        };
      }
    }

    return null;
  }

  private static String pathToAnonymousClass(String searchedText) {
    return pathToAnonymousClass(ourPatternToDetectAnonymousClasses.matcher(searchedText));
  }

  @Nullable
  public static String pathToAnonymousClass(Matcher matcher) {
    if (matcher.matches()) {
      String path = matcher.group(2);
      if (path != null) {
        path = path.trim();
        if (path.endsWith("$") && path.length() >= 2) {
          path = path.substring(0, path.length() - 2);
        }
        if (!path.isEmpty()) return path;
      }
    }

    return null;
  }

  private static String getMemberName(String searchedText) {
    final int index = searchedText.lastIndexOf('#');
    if (index == -1) {
      return null;
    }

    String name = searchedText.substring(index + 1).trim();
    return StringUtil.isEmpty(name) ? null : name;
  }

  public static class Factory implements SearchEverywhereContributorFactory<Object> {

    @NotNull
    @Override
    public SearchEverywhereContributor<Object> createContributor(@NotNull AnActionEvent initEvent) {
      return new ClassSearchEverywhereContributor(initEvent.getProject(), GotoActionBase.getPsiContext(initEvent));
    }
  }

  @NotNull
  static PersistentSearchEverywhereContributorFilter<Language> createLanguageFilter(@NotNull Project project) {
    List<Language> items = Language.getRegisteredLanguages()
      .stream()
      .filter(lang -> lang != Language.ANY && !(lang instanceof DependentLanguage))
      .sorted(LanguageUtil.LANGUAGE_COMPARATOR)
      .collect(Collectors.toList());
    GotoClassSymbolConfiguration persistentConfig = GotoClassSymbolConfiguration.getInstance(project);
    return new PersistentSearchEverywhereContributorFilter<>(items, persistentConfig, Language::getDisplayName, language -> {
      final LanguageFileType fileType = language.getAssociatedFileType();
      return fileType != null ? fileType.getIcon() : null;
    });
  }
}
