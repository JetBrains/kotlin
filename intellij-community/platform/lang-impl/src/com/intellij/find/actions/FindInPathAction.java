
/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

package com.intellij.find.actions;

import com.intellij.find.findInProject.FindInProjectManager;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.psi.PsiDirectoryContainer;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

public class FindInPathAction extends AnAction implements DumbAware {
  public static final NotificationGroup NOTIFICATION_GROUP = NotificationGroup.toolWindowGroup("Find in Path", ToolWindowId.FIND, false);

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    DataContext dataContext = e.getDataContext();
    Project project = e.getData(CommonDataKeys.PROJECT);

    FindInProjectManager findManager = FindInProjectManager.getInstance(project);
    if (!findManager.isEnabled()) {
      showNotAvailableMessage(e, project);
      return;
    }

    findManager.findInProject(dataContext, null);
  }

  static void showNotAvailableMessage(AnActionEvent e, Project project) {
    final String message = "'" + e.getPresentation().getText() + "' is not available while search is in progress";
    NOTIFICATION_GROUP.createNotification(message, NotificationType.WARNING).notify(project);
  }

  @Override
  public void update(@NotNull AnActionEvent e){
    doUpdate(e);
  }

  static void doUpdate(@NotNull AnActionEvent e) {
    Presentation presentation = e.getPresentation();
    Project project = e.getData(CommonDataKeys.PROJECT);
    presentation.setEnabled(project != null);
    if (ActionPlaces.isPopupPlace(e.getPlace())) {
      presentation.setVisible(isValidSearchScope(e));
    }
  }

  private static boolean isValidSearchScope(@NotNull AnActionEvent e) {
    final PsiElement[] elements = e.getData(LangDataKeys.PSI_ELEMENT_ARRAY);
    if (elements != null && elements.length == 1 && elements[0] instanceof PsiDirectoryContainer) {
      return true;
    }
    final VirtualFile[] virtualFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY);
    return virtualFiles != null && virtualFiles.length == 1 && virtualFiles[0].isDirectory();
  }
}
