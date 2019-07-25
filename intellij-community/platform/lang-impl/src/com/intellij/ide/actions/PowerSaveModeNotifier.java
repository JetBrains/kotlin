// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions;

import com.intellij.ide.PowerSaveMode;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.util.Disposer;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.messages.MessageBusConnection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class PowerSaveModeNotifier implements StartupActivity, DumbAware {
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
      Disposer.register(balloon, connection);
      connection.subscribe(PowerSaveMode.TOPIC, () -> notification.expire());
    }
  }
}
