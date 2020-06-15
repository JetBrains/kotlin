// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.service.settings;

import com.intellij.openapi.externalSystem.ExternalSystemManager;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.service.task.ui.AbstractExternalSystemToolWindowFactory;
import com.intellij.openapi.externalSystem.settings.AbstractExternalSystemSettings;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.wm.ToolWindowEP;
import org.jetbrains.annotations.NotNull;

/**
 * @author Denis Zhdanov
 * @deprecated {@link ToolWindowEP#conditionClass} has been deprecated, use {@link AbstractExternalSystemToolWindowFactory#isApplicable(Project)} instead.
 */
@Deprecated
public abstract class AbstractExternalSystemToolWindowCondition implements Condition<Project> {

  @NotNull private final ProjectSystemId myExternalSystemId;

  protected AbstractExternalSystemToolWindowCondition(@NotNull ProjectSystemId externalSystemId) {
    myExternalSystemId = externalSystemId;
  }

  @Override
  public boolean value(Project project) {
    ExternalSystemManager<?, ?, ?, ?, ?> manager = ExternalSystemApiUtil.getManager(myExternalSystemId);
    AbstractExternalSystemSettings<?, ?, ?> settings = manager == null ? null : manager.getSettingsProvider().fun(project);
    return settings != null && !settings.getLinkedProjectsSettings().isEmpty();
  }
}
