// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.dashboard.actions;

import com.intellij.execution.dashboard.RunDashboardServiceViewContributor;
import com.intellij.execution.dashboard.RunDashboardManager;
import com.intellij.execution.dashboard.RunDashboardManagerImpl;
import com.intellij.execution.dashboard.RunDashboardRunConfigurationStatus;
import com.intellij.execution.dashboard.tree.RunDashboardStatusFilter;
import com.intellij.execution.services.ServiceViewActionUtils;
import com.intellij.execution.services.ServiceViewContributor;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CheckedActionGroup;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

import static com.intellij.execution.dashboard.RunDashboardRunConfigurationStatus.*;

public class RunDashboardFilterActionGroup extends DefaultActionGroup implements CheckedActionGroup, DumbAware {
  @SuppressWarnings("unused")
  public RunDashboardFilterActionGroup() {
    this(null, false);
  }

  public RunDashboardFilterActionGroup(@Nullable String shortName, boolean popup) {
    super(shortName, popup);
    RunDashboardRunConfigurationStatus[] statuses = new RunDashboardRunConfigurationStatus[]{STARTED, FAILED, STOPPED, CONFIGURED};
    for (RunDashboardRunConfigurationStatus status : statuses) {
      add(new RunDashboardStatusFilterToggleAction(status));
    }
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    Set<ServiceViewContributor> contributors = e.getData(ServiceViewActionUtils.CONTRIBUTORS_KEY);
    if (contributors != null) {
      for (ServiceViewContributor contributor : contributors) {
        if (contributor instanceof RunDashboardServiceViewContributor) {
          e.getPresentation().setEnabledAndVisible(true);
          return;
        }
      }
    }
    e.getPresentation().setEnabledAndVisible(false);
  }

  private static class RunDashboardStatusFilterToggleAction extends ToggleAction implements DumbAware {
    private final RunDashboardRunConfigurationStatus myStatus;

    RunDashboardStatusFilterToggleAction(RunDashboardRunConfigurationStatus status) {
      super(status.getName());
      myStatus = status;
    }

    @Override
    public boolean isSelected(@NotNull AnActionEvent e) {
      Project project = e.getProject();
      if (project == null) return false;

      RunDashboardStatusFilter statusFilter = ((RunDashboardManagerImpl)RunDashboardManager.getInstance(project)).getStatusFilter();
      return statusFilter.isVisible(myStatus);
    }

    @Override
    public void setSelected(@NotNull AnActionEvent e, boolean state) {
      Project project = e.getProject();
      if (project == null) return;

      RunDashboardManagerImpl manager = (RunDashboardManagerImpl)RunDashboardManager.getInstance(project);
      RunDashboardStatusFilter statusFilter = manager.getStatusFilter();
      if (state) {
        statusFilter.show(myStatus);
      }
      else {
        statusFilter.hide(myStatus);
      }
      manager.updateDashboard(true);
    }
  }
}
