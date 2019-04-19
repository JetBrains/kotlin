// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions;

import com.intellij.ide.SelectInContext;
import com.intellij.ide.SelectInManager;
import com.intellij.ide.SelectInTarget;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import org.jetbrains.annotations.NotNull;

import static com.intellij.openapi.wm.ToolWindowId.PROJECT_VIEW;

/**
 * @author Konstantin Bulenkov
 */
public class SelectInProjectViewAction extends DumbAwareAction {
  private static PsiDocumentManager getDocumentManager(Project project) {
    return project == null || project.isDisposed() ? null : PsiDocumentManager.getInstance(project);
  }

  @Override
  public void beforeActionPerformedUpdate(@NotNull AnActionEvent event) {
    PsiDocumentManager manager = getDocumentManager(event.getProject());
    if (manager != null) manager.commitAllDocuments();
    super.beforeActionPerformedUpdate(event);
  }

  @Override
  public void update(@NotNull AnActionEvent event) {
    SelectInTarget target = SelectInManager.findSelectInTarget(PROJECT_VIEW, event.getProject());
    SelectInContext context = target == null ? null : SelectInContextImpl.createContext(event);
    event.getPresentation().setEnabled(context != null && target.canSelect(context));
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent event) {
    SelectInTarget target = SelectInManager.findSelectInTarget(PROJECT_VIEW, event.getProject());
    SelectInContext context = target == null ? null : SelectInContextImpl.createContext(event);
    if (context != null) {
      target.selectIn(context, true);
    }
  }
}
