/*
 * Copyright 2000-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.openapi.externalSystem.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.statistics.ExternalSystemActionsCollector;
import com.intellij.openapi.externalSystem.util.ExternalSystemBundle;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

/**
 * @author Vladislav.Soroka
 */
public class ShowExternalSystemSettingsAction extends ExternalSystemAction {

  public ShowExternalSystemSettingsAction() {
    getTemplatePresentation().setText(ExternalSystemBundle.messagePointer("action.open.settings.text", "External"));
    getTemplatePresentation().setDescription(ExternalSystemBundle.messagePointer("action.open.settings.description", "external"));
  }

  @Override
  protected boolean isEnabled(@NotNull AnActionEvent e) {
    if (!super.isEnabled(e)) return false;

    ProjectSystemId systemId = getSystemId(e);
    if (systemId == null) return false;

    e.getPresentation().setText(ExternalSystemBundle.messagePointer("action.open.settings.text", systemId.getReadableName()));
    e.getPresentation().setDescription(ExternalSystemBundle.messagePointer("action.open.settings.description", systemId.getReadableName()));
    return true;
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    final ProjectSystemId systemId = getSystemId(e);
    if (systemId != null) {
      Project project = getProject(e);
      ExternalSystemActionsCollector.trigger(project, systemId, this, e);
      showSettingsFor(project, systemId);
    }
  }

  protected static void showSettingsFor(Project project, @NotNull ProjectSystemId systemId) {
    ShowSettingsUtil.getInstance().showSettingsDialog(project, systemId.getReadableName());
  }
}
