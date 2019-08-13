// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions;

import com.intellij.featureStatistics.FeatureUsageTracker;
import com.intellij.ide.actions.searcheverywhere.SymbolSearchEverywhereContributor;
import com.intellij.ide.util.gotoByName.*;
import com.intellij.lang.Language;
import com.intellij.navigation.ChooseByNameRegistry;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.psi.PsiDocumentManager;
import org.jetbrains.annotations.NotNull;

public class GotoSymbolAction extends GotoActionBase implements DumbAware {

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    if (project == null) return;

    boolean dumb = DumbService.isDumb(project);
    if (Registry.is("new.search.everywhere")) {
      if (!dumb || new SymbolSearchEverywhereContributor(project, null).isDumbAware()) {
        showInSearchEverywherePopup(SymbolSearchEverywhereContributor.class.getSimpleName(), e, true, true);
      }
      else {
        GotoClassAction.invokeGoToFile(project, e);
      }
    }
    else {
      if (!dumb) {
        super.actionPerformed(e);
      }
      else {
        GotoClassAction.invokeGoToFile(project, e);
      }
    }
  }

  @Override
  public void gotoActionPerformed(@NotNull AnActionEvent e) {
    FeatureUsageTracker.getInstance().triggerFeatureUsed("navigation.popup.symbol");

    Project project = e.getProject();
    if (project == null) return;

    GotoSymbolModel2 model = new GotoSymbolModel2(project);
    PsiDocumentManager.getInstance(project).commitAllDocuments();
    showNavigationPopup(e, model, new GotoActionCallback<Language>() {
      @Override
      protected ChooseByNameFilter<Language> createFilter(@NotNull ChooseByNamePopup popup) {
        return new ChooseByNameLanguageFilter(popup, model, GotoClassSymbolConfiguration.getInstance(project), project);
      }

      @Override
      public void elementChosen(ChooseByNamePopup popup, Object element) {
        GotoClassAction.handleSubMemberNavigation(popup, element);
      }
    }, "Symbols matching patterns", true);
  }

  @Override
  protected boolean hasContributors(@NotNull DataContext dataContext) {
    return ChooseByNameRegistry.getInstance().getSymbolModelContributors().size() > 0;
  }
}