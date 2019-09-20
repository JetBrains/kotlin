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

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationListener;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.externalSystem.service.notification.NotificationData;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.playback.commands.ActionCommand;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.gradle.codeInsight.actions.AddGradleDslPluginAction;

import javax.swing.event.HyperlinkEvent;

/**
 * @author Vladislav.Soroka
 */
public class ApplyGradlePluginCallback extends NotificationListener.Adapter {

  public final static String ID = "apply_gradle_plugin";

  private final NotificationData myNotificationData;
  private final Project myProject;

  public ApplyGradlePluginCallback(NotificationData notificationData, Project project) {
    myNotificationData = notificationData;
    myProject = project;
  }

  @Override
  protected void hyperlinkActivated(@NotNull Notification notification, @NotNull HyperlinkEvent event) {
    new GotoSourceNotificationCallback(myNotificationData, myProject).hyperlinkActivated(notification, event);

    final AnAction action = ActionManager.getInstance().getAction(AddGradleDslPluginAction.ID);
    assert action instanceof AddGradleDslPluginAction;
    final AddGradleDslPluginAction addGradleDslPluginAction = (AddGradleDslPluginAction)action;
    ActionManager.getInstance().tryToExecute(
      addGradleDslPluginAction, ActionCommand.getInputEvent(AddGradleDslPluginAction.ID), null, ActionPlaces.UNKNOWN, true);
  }
}
