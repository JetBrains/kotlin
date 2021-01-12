package com.intellij.openapi.externalSystem.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.externalSystem.ExternalSystemUiAware;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.model.project.ExternalConfigPathAware;
import com.intellij.openapi.externalSystem.model.project.ExternalEntityData;
import com.intellij.openapi.externalSystem.util.ExternalSystemBundle;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

/**
 * @author Vladislav.Soroka
 */
public class OpenExternalConfigAction extends ExternalSystemNodeAction<ExternalConfigPathAware> {

  public OpenExternalConfigAction() {
    super(ExternalConfigPathAware.class);
    getTemplatePresentation().setText(ExternalSystemBundle.messagePointer("action.open.config.text", "External"));
    getTemplatePresentation().setDescription(ExternalSystemBundle.messagePointer("action.open.config.description", "external"));
  }

  @Override
  protected boolean isEnabled(@NotNull AnActionEvent e) {
    if (!super.isEnabled(e)) return false;

    final ExternalEntityData externalData = getExternalData(e, ExternalEntityData.class);
    if (!(externalData instanceof ExternalConfigPathAware)) return false;

    VirtualFile config = getExternalConfig((ExternalConfigPathAware)externalData, externalData.getOwner());
    if (config == null) return false;

    ProjectSystemId externalSystemId = getSystemId(e);
    e.getPresentation().setText(ExternalSystemBundle.messagePointer("action.open.config.text", externalSystemId.getReadableName()));
    e.getPresentation().setDescription(ExternalSystemBundle.messagePointer("action.open.config.description", externalSystemId.getReadableName()));
    final ExternalSystemUiAware uiAware = getExternalSystemUiAware(e);
    if (uiAware != null) {
      e.getPresentation().setIcon(uiAware.getProjectIcon());
    }

    return true;
  }

  @Override
  protected void perform(@NotNull Project project,
                         @NotNull ProjectSystemId systemId,
                         @NotNull ExternalConfigPathAware configPathAware,
                         @NotNull AnActionEvent e) {
    VirtualFile configFile = getExternalConfig(configPathAware, systemId);
    if (configFile != null) {
      OpenFileDescriptor descriptor = new OpenFileDescriptor(project, configFile);
      FileEditorManager.getInstance(project).openTextEditor(descriptor, true);
    }
  }
}
