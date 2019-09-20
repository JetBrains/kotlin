// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.dashboard.actions;

import com.intellij.execution.ExecutionManager;
import com.intellij.execution.Executor;
import com.intellij.execution.dashboard.RunDashboardRunConfigurationNode;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.execution.ui.RunContentManagerImpl;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.ui.content.Content;
import org.jetbrains.annotations.NotNull;

public class ClearContentAction extends DumbAwareAction {
  @Override
  public void update(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    RunDashboardRunConfigurationNode node = project == null ? null : RunDashboardActionUtils.getTarget(e);
    boolean enabled = node != null && node.getContent() != null;
    e.getPresentation().setEnabled(enabled);
    e.getPresentation().setVisible(enabled || !ActionPlaces.isPopupPlace(e.getPlace()));
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    if (project == null) return;

    RunDashboardRunConfigurationNode node = RunDashboardActionUtils.getTarget(e);
    if (node == null) return;

    Content content = node.getContent();
    if (content == null) return;

    RunContentDescriptor descriptor = node.getDescriptor();
    if (descriptor == null) return;

    Executor executor = RunContentManagerImpl.getExecutorByContent(content);
    if (executor == null) return;

    ExecutionManager.getInstance(project).getContentManager().removeRunContent(executor, descriptor);
  }
}
