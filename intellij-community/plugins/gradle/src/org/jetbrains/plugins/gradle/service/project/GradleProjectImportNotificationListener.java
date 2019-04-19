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
package org.jetbrains.plugins.gradle.service.project;

import com.intellij.ide.util.PsiNavigationSupport;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListenerAdapter;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskType;
import com.intellij.openapi.externalSystem.service.notification.ExternalSystemNotificationManager;
import com.intellij.openapi.externalSystem.service.notification.NotificationCategory;
import com.intellij.openapi.externalSystem.service.notification.NotificationData;
import com.intellij.openapi.externalSystem.service.notification.NotificationSource;
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.Navigatable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.util.GradleConstants;

import java.io.File;

import static org.jetbrains.plugins.gradle.tooling.ErrorMessageBuilder.*;

/**
 * @author Vladislav.Soroka
 */
public class GradleProjectImportNotificationListener extends ExternalSystemTaskNotificationListenerAdapter {
  @Override
  public void onTaskOutput(@NotNull ExternalSystemTaskId id, @NotNull String text, boolean stdOut) {
    if (!stdOut && !StringUtil.isEmpty(text) &&
        GradleConstants.SYSTEM_ID.getId().equals(id.getProjectSystemId().getId()) &&
        id.getType() == ExternalSystemTaskType.RESOLVE_PROJECT) {
      final Project project = id.findProject();
      if (project != null) {
        if (StringUtil.startsWith(text, GROUP_TAG)) {
          final String group = substringBetween(text, GROUP_TAG, GROUP_TAG);
          if (StringUtil.isEmpty(group)) return;

          int start = (GROUP_TAG.length() * 2) + group.length();

          String path = null;
          String errorMessage = text.substring(start);
          if (StringUtil.startsWith(errorMessage, NAV_TAG)) {
            path = substringBetween(errorMessage, NAV_TAG, NAV_TAG);
            if (!StringUtil.isEmpty(path)) {
              start += (NAV_TAG.length() * 2) + path.length();
            }
          }

          errorMessage = text.substring(start).replaceAll(EOL_TAG, "\n");
          NotificationData notification = new NotificationData(
            group, errorMessage,
            StringUtil.equals("Performance statistics", group) ? NotificationCategory.INFO : NotificationCategory.WARNING,
            NotificationSource.PROJECT_SYNC);

          if (path != null) {
            notification.setNavigatable(new MyNavigatable(new File(path), project));
          }

          ExternalSystemNotificationManager.getInstance(project).showNotification(id.getProjectSystemId(), notification);
        }
      }
    }
  }

  @Nullable
  private static String substringBetween(@NotNull String str, @NotNull String open, @NotNull String close) {
    int start = str.indexOf(open);
    if (start == -1) return null;
    int end = str.indexOf(close, start + open.length());
    return end != -1 ? str.substring(start + open.length(), end) : null;
  }

  private static class MyNavigatable implements Navigatable {

    private final @NotNull File myFile;
    private final @NotNull Project myProject;
    private volatile Navigatable openFileDescriptor;

    MyNavigatable(@NotNull File file, @NotNull Project project) {
      myFile = file;
      myProject = project;
    }

    @Override
    public void navigate(boolean requestFocus) {
      Navigatable fileDescriptor = openFileDescriptor;
      if (fileDescriptor == null) {
        final VirtualFile virtualFile = ExternalSystemUtil.findLocalFileByPath(myFile.getPath());
        if (virtualFile != null) {
          openFileDescriptor = fileDescriptor = PsiNavigationSupport.getInstance().createNavigatable(myProject, virtualFile, -1);
        }
      }
      if (fileDescriptor != null && fileDescriptor.canNavigate()) {
        fileDescriptor.navigate(requestFocus);
      }
    }

    @Override
    public boolean canNavigate() {
      return myFile.exists();
    }

    @Override
    public boolean canNavigateToSource() {
      return canNavigate();
    }
  }
}
