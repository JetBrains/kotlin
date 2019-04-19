// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.externalSystem.ExternalSystemUiAware;
import com.intellij.openapi.externalSystem.model.ExternalSystemDataKeys;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.model.project.ExternalConfigPathAware;
import com.intellij.openapi.externalSystem.service.settings.ExternalSystemConfigLocator;
import com.intellij.openapi.externalSystem.statistics.ExternalSystemActionsCollector;
import com.intellij.openapi.externalSystem.view.ExternalSystemNode;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Vladislav.Soroka
 */
public abstract class ExternalSystemNodeAction<T> extends ExternalSystemAction {

  private final Class<T> myExternalDataClazz;

  public ExternalSystemNodeAction(Class<T> externalDataClazz) {
    super();
    myExternalDataClazz = externalDataClazz;
  }

  @Override
  protected boolean isEnabled(@NotNull AnActionEvent e) {
    return super.isEnabled(e) && getSystemId(e) != null && getExternalData(e, myExternalDataClazz) != null;
  }

  protected abstract void perform(@NotNull Project project,
                                  @NotNull ProjectSystemId projectSystemId,
                                  @NotNull T externalData,
                                  @NotNull AnActionEvent e);

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    final Project project = getProject(e);
    if (project == null) return;

    ProjectSystemId projectSystemId = getSystemId(e);
    if (projectSystemId == null) return;

    final T data = getExternalData(e, myExternalDataClazz);
    if (data == null) return;

    ExternalSystemActionsCollector.trigger(project, projectSystemId, this, e);
    perform(project, projectSystemId, data, e);
  }

  @Nullable
  protected ExternalSystemUiAware getExternalSystemUiAware(@NotNull AnActionEvent e) {
    return e.getData(ExternalSystemDataKeys.UI_AWARE);
  }

  @SuppressWarnings("unchecked")
  @Nullable
  protected <T> T getExternalData(@NotNull AnActionEvent e, Class<T> dataClass) {
    ExternalSystemNode node = ContainerUtil.getFirstItem(e.getData(ExternalSystemDataKeys.SELECTED_NODES));
    return node != null && dataClass.isInstance(node.getData()) ? (T)node.getData() : null;
  }

  protected boolean isIgnoredNode(@NotNull AnActionEvent e) {
    ExternalSystemNode node = ContainerUtil.getFirstItem(e.getData(ExternalSystemDataKeys.SELECTED_NODES));
    return node != null && myExternalDataClazz.isInstance(node.getData()) && node.isIgnored();
  }

  @Nullable
  protected VirtualFile getExternalConfig(@NotNull ExternalConfigPathAware data, ProjectSystemId externalSystemId) {
    String path = data.getLinkedExternalProjectPath();
    LocalFileSystem fileSystem = LocalFileSystem.getInstance();
    VirtualFile externalSystemConfigPath = fileSystem.refreshAndFindFileByPath(path);
    if (externalSystemConfigPath == null) {
      return null;
    }

    VirtualFile toOpen = externalSystemConfigPath;
    for (ExternalSystemConfigLocator locator : ExternalSystemConfigLocator.EP_NAME.getExtensions()) {
      if (externalSystemId.equals(locator.getTargetExternalSystemId())) {
        toOpen = locator.adjust(toOpen);
        if (toOpen == null) {
          return null;
        }
        break;
      }
    }
    return toOpen.isDirectory() ? null : toOpen;
  }
}