// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.action;

import com.intellij.icons.AllIcons;
import com.intellij.ide.actions.ImportModuleAction;
import com.intellij.ide.util.newProjectWizard.AddModuleWizard;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.externalSystem.ExternalSystemManager;
import com.intellij.openapi.externalSystem.model.ExternalSystemDataKeys;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.service.project.wizard.AbstractExternalProjectImportProvider;
import com.intellij.openapi.externalSystem.statistics.ExternalSystemActionsCollector;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.externalSystem.util.ExternalSystemBundle;
import com.intellij.openapi.project.Project;
import com.intellij.projectImport.ProjectImportProvider;
import org.jetbrains.annotations.NotNull;

/**
 * @author Denis Zhdanov
 */
public class AttachExternalProjectAction extends AnAction {

  public AttachExternalProjectAction() {
    getTemplatePresentation().setText(ExternalSystemBundle.message("action.attach.external.project.text", "external"));
    getTemplatePresentation().setDescription(ExternalSystemBundle.message("action.attach.external.project.description", "external"));
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    Presentation presentation = e.getPresentation();
    // todo [Vlad, IDEA-187835]: provide java subsystem independent implementation
    if (!ExternalSystemApiUtil.isJavaCompatibleIde()) {
      presentation.setEnabledAndVisible(false);
      return;
    }

    ProjectSystemId externalSystemId = e.getData(ExternalSystemDataKeys.EXTERNAL_SYSTEM_ID);
    if (externalSystemId != null) {
      String name = externalSystemId.getReadableName();
      presentation.setText(ExternalSystemBundle.message("action.attach.external.project.text", name));
      presentation.setDescription(ExternalSystemBundle.message("action.attach.external.project.description", name));
    }

    presentation.setIcon(AllIcons.General.Add);
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    ProjectSystemId externalSystemId = e.getData(ExternalSystemDataKeys.EXTERNAL_SYSTEM_ID);
    if (externalSystemId == null) {
      return;
    }

    ExternalSystemManager<?,?,?,?,?> manager = ExternalSystemApiUtil.getManager(externalSystemId);
    if (manager == null) {
      return;
    }

    Project project = e.getProject();
    if (project == null) {
      return;
    }
    ExternalSystemActionsCollector.trigger(project, externalSystemId, this, e);
    
    ProjectImportProvider[] projectImportProviders = new ProjectImportProvider[1];
    for (ProjectImportProvider provider : ProjectImportProvider.PROJECT_IMPORT_PROVIDER.getExtensions()) {
      if (provider instanceof AbstractExternalProjectImportProvider
          && externalSystemId.equals(((AbstractExternalProjectImportProvider)provider).getExternalSystemId()))
      {
        projectImportProviders[0] = provider;
        break;
      }
    }
    if (projectImportProviders[0] == null) {
      return;
    }

    AddModuleWizard wizard = ImportModuleAction.selectFileAndCreateWizard(project,
                                                                          null,
                                                                          manager.getExternalProjectDescriptor(),
                                                                          projectImportProviders);
    if (wizard != null && (wizard.getStepCount() <= 0 || wizard.showAndGet())) {
      ImportModuleAction.createFromWizard(project, wizard);
      wizard.disposeIfNeeded();
    }
  }
}
