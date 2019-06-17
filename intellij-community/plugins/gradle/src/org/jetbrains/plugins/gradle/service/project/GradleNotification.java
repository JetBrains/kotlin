/*
 * Copyright 2000-2013 JetBrains s.r.o.
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

import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationListener;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Vladislav.Soroka
 */
public class GradleNotification {
  public static final NotificationGroup NOTIFICATION_GROUP = new NotificationGroup("Gradle Notification Group",
                                                                                    NotificationDisplayType.STICKY_BALLOON,
                                                                                    true);

  @NotNull private final Project myProject;

  @NotNull
  public static GradleNotification getInstance(@NotNull Project project) {
    return ServiceManager.getService(project, GradleNotification.class);
  }

  public GradleNotification(@NotNull Project project) {
    myProject = project;
  }

  public void showBalloon(@NotNull final String title,
                          @NotNull final String message,
                          @NotNull final NotificationType type,
                          @Nullable final NotificationListener listener) {
    NOTIFICATION_GROUP.createNotification(title, message, type, listener).notify(myProject);
  }
}

