// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.internal.psiView;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Konstantin Bulenkov
 */
public class PsiViewerAction extends DumbAwareAction {
  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Editor editor = isForContext() ? e.getData(CommonDataKeys.EDITOR) : null;
    Project project = e.getProject();
    assert project != null;
    new PsiViewerDialog(project, editor).show();
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    boolean enabled = isEnabled(e.getProject());
    e.getPresentation().setEnabledAndVisible(enabled);
    if (enabled && isForContext() && e.getData(CommonDataKeys.EDITOR) == null) {
      e.getPresentation().setEnabled(false);
    }
  }

  protected boolean isForContext() {
    return false;
  }

  private static boolean isEnabled(@Nullable Project project) {
    if (project == null) return false;
    if (ApplicationManager.getApplication().isInternal()) return true;
    for (Module module : ModuleManager.getInstance(project).getModules()) {
      if ("PLUGIN_MODULE".equals(ModuleType.get(module).getId())) {
        return true;
      }
    }
    return false;
  }

  public static class ForContext extends PsiViewerAction {
    @Override
    protected boolean isForContext() {
      return true;
    }
  }
}