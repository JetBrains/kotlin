// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions.searcheverywhere;

import com.intellij.ide.IdeBundle;
import com.intellij.ide.actions.GotoActionBase;
import com.intellij.ide.util.gotoByName.FilteringGotoByModel;
import com.intellij.ide.util.gotoByName.GotoClassSymbolConfiguration;
import com.intellij.ide.util.gotoByName.GotoSymbolModel2;
import com.intellij.lang.DependentLanguage;
import com.intellij.lang.Language;
import com.intellij.lang.LanguageUtil;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.ui.IdeUICustomization;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Konstantin Bulenkov
 */
public class SymbolSearchEverywhereContributor extends AbstractGotoSEContributor<Language> {

  public SymbolSearchEverywhereContributor(@Nullable Project project, @Nullable PsiElement context) {
    super(project, context);
  }

  @NotNull
  @Override
  public String getGroupName() {
    return "Symbols";
  }

  @Override
  public String includeNonProjectItemsText() {
    return IdeBundle.message("checkbox.include.non.project.symbols", IdeUICustomization.getInstance().getProjectConceptName());
  }

  @Override
  public int getSortWeight() {
    return 300;
  }

  @Override
  public boolean isDumbModeSupported() {
    return false;
  }

  @NotNull
  @Override
  protected FilteringGotoByModel<Language> createModel(@NotNull Project project) {
    return new GotoSymbolModel2(project);
  }

  public static class Factory implements SearchEverywhereContributorFactory<Object, Language> {
    @NotNull
    @Override
    public SearchEverywhereContributor<Object, Language> createContributor(@NotNull AnActionEvent initEvent) {
      return new SymbolSearchEverywhereContributor(initEvent.getProject(), GotoActionBase.getPsiContext(initEvent));
    }

    @Nullable
    @Override
    public SearchEverywhereContributorFilter<Language> createFilter(@NotNull AnActionEvent initEvent) {
      Project project = initEvent.getProject();
      if (project == null) {
        return null;
      }

      List<Language> items = Language.getRegisteredLanguages()
                                     .stream()
                                     .filter(lang -> lang != Language.ANY && !(lang instanceof DependentLanguage))
                                     .sorted(LanguageUtil.LANGUAGE_COMPARATOR)
                                     .collect(Collectors.toList());
      return new PersistentSearchEverywhereContributorFilter<>(items, GotoClassSymbolConfiguration.getInstance(project),
                                                               ClassSearchEverywhereContributor.Factory.LANGUAGE_NAME_EXTRACTOR,
                                                               ClassSearchEverywhereContributor.Factory.LANGUAGE_ICON_EXTRACTOR
      );
    }
  }

}
