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
import com.intellij.openapi.externalSystem.service.notification.NotificationData;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import javax.swing.event.HyperlinkEvent;

/**
 * @author Vladislav.Soroka
 */
public class GotoSourceNotificationCallback extends NotificationListener.Adapter {
  public final static String ID = "goto_source";

  private final NotificationData myNotificationData;
  private final Project myProject;

  public GotoSourceNotificationCallback(NotificationData notificationData, Project project) {
    myNotificationData = notificationData;
    myProject = project;
  }

  @Override
  protected void hyperlinkActivated(@NotNull Notification notification, @NotNull HyperlinkEvent event) {
    if (myNotificationData.getFilePath() == null) return;
    VirtualFile virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(myNotificationData.getFilePath());
    assert virtualFile != null;
    int line = myNotificationData.getLine() - 1;
    int column = myNotificationData.getColumn() - 1;

    final int guiLine = line < 0 ? -1 : line;
    final int guiColumn = column < 0 ? -1 : column + 1;
    new OpenFileDescriptor(myProject, virtualFile, guiLine, guiColumn).navigate(true);
  }
}
