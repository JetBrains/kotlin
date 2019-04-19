/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.intellij.ide.actions;

import com.intellij.ide.PowerSaveMode;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.util.Disposer;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.messages.MessageBusConnection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author peter
 */
public class PowerSaveModeNotifier implements StartupActivity {
  private static final NotificationGroup POWER_SAVE_MODE = NotificationGroup.balloonGroup("Power Save Mode");
  private static final String IGNORE_POWER_SAVE_MODE = "ignore.power.save.mode";

  @Override
  public void runActivity(@NotNull Project project) {
    if (PowerSaveMode.isEnabled()) {
      notifyOnPowerSaveMode(project);
    }
  }

  static void notifyOnPowerSaveMode(@Nullable Project project) {
    if (PropertiesComponent.getInstance().getBoolean(IGNORE_POWER_SAVE_MODE)) {
      return;
    }

    Notification notification = POWER_SAVE_MODE
      .createNotification("Power save mode is on", "Code insight and background tasks are disabled.", NotificationType.WARNING, null);

    notification.addAction(new NotificationAction("Do Not Show Again") {
      @Override
      public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification notification) {
        PropertiesComponent.getInstance().setValue(IGNORE_POWER_SAVE_MODE, true);
        notification.expire();
      }
    });
    notification.addAction(new NotificationAction("Disable Power Save Mode") {
      @Override
      public void actionPerformed(@NotNull AnActionEvent e, @NotNull Notification notification) {
        PowerSaveMode.setEnabled(false);
        notification.expire();
      }
    });

    notification.notify(project);

    Balloon balloon = notification.getBalloon();
    if (balloon != null) {
      MessageBus bus = project == null ? ApplicationManager.getApplication().getMessageBus() : project.getMessageBus();
      MessageBusConnection connection = bus.connect();
      connection.subscribe(PowerSaveMode.TOPIC, () -> notification.expire());
      Disposer.register(balloon, connection);
    }
  }
}
