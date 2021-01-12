// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions;

import com.intellij.ide.actions.searcheverywhere.SymbolSearchEverywhereContributor;
import com.intellij.navigation.ChooseByNameRegistry;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class GotoSymbolAction extends SearchEverywhereBaseAction implements DumbAware {

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    if (project == null) return;

    boolean dumb = DumbService.isDumb(project);
    if (!dumb || new SymbolSearchEverywhereContributor(e).isDumbAware()) {
      showInSearchEverywherePopup(SymbolSearchEverywhereContributor.class.getSimpleName(), e, true, true);
    }
    else {
      GotoClassAction.invokeGoToFile(project, e);
    }
  }

  @Override
  protected boolean hasContributors(@NotNull DataContext dataContext) {
    return ChooseByNameRegistry.getInstance().getSymbolModelContributors().size() > 0;
  }
}