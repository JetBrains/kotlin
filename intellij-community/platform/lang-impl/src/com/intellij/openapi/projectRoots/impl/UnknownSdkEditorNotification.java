// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.projectRoots.impl;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.impl.UnknownSdkResolver.DownloadSdkFix;
import com.intellij.openapi.projectRoots.impl.UnknownSdkResolver.UnknownSdk;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.EditorNotificationPanel;
import com.intellij.util.SmartList;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UnknownSdkEditorNotification implements Disposable {
  public static final Key<List<MissingSdkNotificationPanel>> NOTIFICATIONS = Key.create("notifications added to the editor");
  private static final Key<?> EDITOR_NOTIFICATIONS_KEY = Key.create("SdkSetupNotificationNew");

  @NotNull
  public static UnknownSdkEditorNotification getInstance(@NotNull Project project) {
    return project.getService(UnknownSdkEditorNotification.class);
  }

  private final Project myProject;
  private final FileEditorManager myFileEditorManager;
  private Map<UnknownSdk, DownloadSdkFix> myNotifications = new HashMap<>();

  UnknownSdkEditorNotification(@NotNull Project project) {
    myProject = project;
    myFileEditorManager = FileEditorManager.getInstance(myProject);
    myProject.getMessageBus()
      .connect(this)
      .subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, new FileEditorManagerListener() {
        @Override
        public void fileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
          for (FileEditor editor : myFileEditorManager.getEditors(file)) {
            updateEditorNotifications(editor);
          }
        }
      });
  }

  private static class MissingSdkNotificationPanel extends EditorNotificationPanel {
    private final UnknownSdk myInfo;

    private MissingSdkNotificationPanel(@NotNull final UnknownSdk info) {
      myInfo = info;
    }

    public boolean isSameProblemAs(@NotNull MissingSdkNotificationPanel panel) {
      return this.myInfo.equals(panel.myInfo);
    }
  }

  @NotNull
  private MissingSdkNotificationPanel createPanelFor(@NotNull UnknownSdk info,
                                                     @NotNull DownloadSdkFix fix) {
    String sdkName = info.getSdkType().getPresentableName();

    MissingSdkNotificationPanel panel = new MissingSdkNotificationPanel(info);
    panel.setProject(myProject);
    panel.setProviderKey(EDITOR_NOTIFICATIONS_KEY);
    panel.setText(sdkName + " \"" + info.getSdkName() + "\" is missing");

    panel.createActionLabel("Download " + sdkName + " (" + fix.getDownloadDescription() + ")", () -> {
      removeNotification(panel);
      UnknownSdkTracker.getInstance(myProject).applyDownloadableFix(info, fix);
    });

    panel.createActionLabel("Configure...", () -> {
      UnknownSdkTracker.getInstance(myProject).showSdkSelectionPopup(info, panel, () -> removeNotification(panel));
    });

    return panel;
  }

  @Override
  public void dispose() { }

  public void showNotifications(@NotNull final Map<? extends UnknownSdk, DownloadSdkFix> files) {
    myNotifications = new HashMap<>(files);

    for (FileEditor editor : myFileEditorManager.getAllEditors()) {
      updateEditorNotifications(editor);
    }
  }

  private void removeNotification(@NotNull MissingSdkNotificationPanel expiredPanel) {
    myNotifications.remove(expiredPanel.myInfo);
    for (FileEditor editor : myFileEditorManager.getAllEditors()) {
      List<MissingSdkNotificationPanel> notifications = editor.getUserData(NOTIFICATIONS);
      if (notifications == null) continue;
      for (MissingSdkNotificationPanel panel : new ArrayList<>(notifications)) {
        if (panel.isSameProblemAs(expiredPanel)) {
          myFileEditorManager.removeTopComponent(editor, panel);
          notifications.remove(panel);
        }
      }
    }
  }

  private void updateEditorNotifications(@NotNull FileEditor editor) {
    if (!editor.isValid()) return;

    List<MissingSdkNotificationPanel> notifications = editor.getUserData(NOTIFICATIONS);
    if (notifications != null) {
      for (JComponent component : notifications) {
        myFileEditorManager.removeTopComponent(editor, component);
      }
      notifications.clear();
    } else {
      notifications = new SmartList<>();
      editor.putUserData(NOTIFICATIONS, notifications);
    }

    for (Map.Entry<UnknownSdk, DownloadSdkFix> e : myNotifications.entrySet()) {
      MissingSdkNotificationPanel notification = createPanelFor(e.getKey(), e.getValue());

      notifications.add(notification);
      myFileEditorManager.addTopComponent(editor, notification);
    }
  }
}
