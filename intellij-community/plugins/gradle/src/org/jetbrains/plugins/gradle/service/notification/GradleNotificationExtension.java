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
package org.jetbrains.plugins.gradle.service.notification;

import com.intellij.execution.rmi.RemoteUtil;
import com.intellij.openapi.externalSystem.model.ExternalSystemException;
import com.intellij.openapi.externalSystem.model.LocationAwareExternalSystemException;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.service.notification.ExternalSystemNotificationExtension;
import com.intellij.openapi.externalSystem.service.notification.NotificationData;
import com.intellij.openapi.externalSystem.service.notification.callback.OpenExternalSystemSettingsCallback;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtilRt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.util.GradleConstants;

import java.io.ObjectStreamException;

/**
 * @author Vladislav.Soroka
 */
public class GradleNotificationExtension implements ExternalSystemNotificationExtension {
  @NotNull
  @Override
  public ProjectSystemId getTargetExternalSystemId() {
    return GradleConstants.SYSTEM_ID;
  }

  @Override
  public boolean isInternalError(@NotNull Throwable error) {
    Throwable unwrapped = RemoteUtil.unwrap(error);
    String message = unwrapped.getMessage();
    if ("Compilation failed; see the compiler error output for details.".equals(message)) {
      // compiler errors should be handled by BuildOutputParsers
      return true;
    }
    if (unwrapped.getCause() instanceof ObjectStreamException) {
      // gradle tooling internal serialization issues
      return true;
    }
    if (unwrapped instanceof ExternalSystemException) {
      Throwable cause = unwrapped.getCause();
      if (cause != null) {
        String name = cause.getClass().getName();
        if (name.startsWith("groovy.lang.") || // Gradle Groovy DSL errors should be handled by GradleBuildScriptErrorParser
            name.startsWith("org.gradle.")) {
          return ((ExternalSystemException)unwrapped).getQuickFixes().length == 0;
        }
      }

      if (unwrapped instanceof LocationAwareExternalSystemException) {
        String filePath = ((LocationAwareExternalSystemException)unwrapped).getFilePath();
        // avoid build tw duplicating messages related to build script errors
        // Gradle build script errors are better described by the build output and should be handled by GradleBuildScriptErrorParser
        if (FileUtilRt.extensionEquals(filePath, GradleConstants.EXTENSION) ||
            FileUtilRt.extensionEquals(filePath, GradleConstants.KOTLIN_DSL_SCRIPT_EXTENSION)) {
          return ((LocationAwareExternalSystemException)unwrapped).getQuickFixes().length == 0;
        }
      }
    }
    return false;
  }

  @Override
  public void customize(@NotNull NotificationData notification,
                        @NotNull Project project,
                        @Nullable Throwable error) {
    if (error == null) return;
    Throwable unwrapped = RemoteUtil.unwrap(error);
    if (unwrapped instanceof ExternalSystemException) {
      updateNotification(notification, project, (ExternalSystemException)unwrapped);
    }
  }

  protected void updateNotification(@NotNull final NotificationData notificationData,
                                    @NotNull final Project project,
                                    @NotNull ExternalSystemException e) {
    for (String fix : e.getQuickFixes()) {
      if (OpenGradleSettingsCallback.ID.equals(fix)) {
        notificationData.setListener(OpenGradleSettingsCallback.ID, new OpenGradleSettingsCallback(project));
      }
      else if (GotoSourceNotificationCallback.ID.equals(fix)) {
        notificationData.setListener(GotoSourceNotificationCallback.ID, new GotoSourceNotificationCallback(notificationData, project));
      }
      else if (OpenExternalSystemSettingsCallback.ID.equals(fix)) {
        String linkedProjectPath = e instanceof LocationAwareExternalSystemException ?
                                   ((LocationAwareExternalSystemException)e).getFilePath() : null;
        notificationData.setListener(
          OpenExternalSystemSettingsCallback.ID,
          new OpenExternalSystemSettingsCallback(project, GradleConstants.SYSTEM_ID, linkedProjectPath)
        );
      }
    }
  }
}
