// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.packageDependencies.actions;

import com.intellij.analysis.AnalysisScope;
import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.module.Module;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;

public class AnalyzeDependenciesOnSpecifiedTargetAction extends AnAction {
  public static final DataKey<GlobalSearchScope> TARGET_SCOPE_KEY = DataKey.create("MODULE_DEPENDENCIES_TARGET_SCOPE");

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    final Module module = e.getData(LangDataKeys.MODULE_CONTEXT);
    final GlobalSearchScope targetScope = e.getData(TARGET_SCOPE_KEY);
    if (module == null || targetScope == null) return;

    new AnalyzeDependenciesOnSpecifiedTargetHandler(module.getProject(), new AnalysisScope(module), targetScope).analyze();
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    final Module module = e.getData(LangDataKeys.MODULE_CONTEXT);
    final GlobalSearchScope scope = e.getData(TARGET_SCOPE_KEY);
    final Presentation presentation = e.getPresentation();
    if (module != null && scope != null) {
      presentation.setVisible(true);
      presentation.setText(CodeInsightBundle.message("analyze.dependencies.on.scope.action.text", module.getName(), scope.getDisplayName()));
    }
    else {
      presentation.setVisible(false);
    }
  }
}
