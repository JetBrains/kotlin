// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.notification;

import com.intellij.openapi.externalSystem.model.ExternalSystemException;
import com.intellij.openapi.externalSystem.service.notification.NotificationData;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

/**
 * @author Vladislav.Soroka
 */
public class GradleNotificationJavaExtension extends GradleNotificationExtension {
  @Override
  protected void updateNotification(@NotNull NotificationData notificationData,
                                    @NotNull Project project,
                                    @NotNull ExternalSystemException e) {
    for (String fix : e.getQuickFixes()) {
      if (ApplyGradlePluginCallback.ID.equals(fix)) {
        notificationData.setListener(ApplyGradlePluginCallback.ID, new ApplyGradlePluginCallback(notificationData, project));
      }
    }
  }
}
