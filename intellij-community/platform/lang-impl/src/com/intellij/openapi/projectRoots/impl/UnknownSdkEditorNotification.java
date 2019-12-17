// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.projectRoots.impl;

import com.google.common.collect.ImmutableList;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.SdkType;
import com.intellij.openapi.projectRoots.impl.UnknownSdkResolver.DownloadSdkFix;
import com.intellij.openapi.projectRoots.impl.UnknownSdkResolver.UnknownSdk;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.EditorNotificationPanel;
import com.intellij.util.SmartList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

public class UnknownSdkEditorNotification implements Disposable {
  public static final Key<List<MissingSdkNotificationPanel>> NOTIFICATIONS = Key.create("notifications added to the editor");
  private static final Key<?> EDITOR_NOTIFICATIONS_KEY = Key.create("SdkSetupNotificationNew");

  @NotNull
  public static UnknownSdkEditorNotification getInstance(@NotNull Project project) {
    return project.getService(UnknownSdkEditorNotification.class);
  }

  private final Project myProject;
  private final FileEditorManager myFileEditorManager;
  private final Map<String, SdkFixInfo> myNotifications = new TreeMap<>();

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

  @NotNull
  private MissingSdkNotificationPanel createPanelFor(@NotNull SdkFixInfo info) {
    Pair<UnknownSdk, DownloadSdkFix> fixInfo = info.getFixInfo();
    String sdkName = info.getSdkName();

    SdkType sdkType = fixInfo != null ? fixInfo.first.getSdkType() : null;
    String sdkTypeName = sdkType != null ? sdkType.getPresentableName() : "SDK";

    MissingSdkNotificationPanel panel = new MissingSdkNotificationPanel(info);
    panel.setProject(myProject);
    panel.setProviderKey(EDITOR_NOTIFICATIONS_KEY);
    panel.setText(sdkTypeName + " \"" + sdkName + "\" is missing");

    if (fixInfo != null) {
      UnknownSdk unknownSdk = fixInfo.first;
      DownloadSdkFix fix = fixInfo.second;

      panel.createActionLabel("Download " + sdkTypeName + " (" + fix.getDownloadDescription() + ")", () -> {
        removeNotification(panel);
        UnknownSdkTracker.getInstance(myProject).applyDownloadableFix(unknownSdk, fix);
      });
    }

    panel.createActionLabel("Configure...",
                            () -> {
                              //FileEditorManager#addTopComponent wraps the panel to implement borders, unwrapping
                              Container container = panel.getParent();
                              if (container == null) container = panel;

                              UnknownSdkTracker
                                .getInstance(myProject)
                                .showSdkSelectionPopup(sdkName, sdkType, container, () -> removeNotification(panel));
                            }
    );

    return panel;
  }

  @Override
  public void dispose() { }

  @NotNull
  public List<SdkFixInfo> getNotifications() {
    return ImmutableList.copyOf(myNotifications.values());
  }

  public void showNotifications(@NotNull List<String> unifiableSdkNames,
                                @NotNull Map<UnknownSdk, DownloadSdkFix> files) {
    myNotifications.clear();

    for (String name : unifiableSdkNames) {
      myNotifications.put(name, SdkFixInfo.forBroken(name));
    }

    for (Map.Entry<UnknownSdk, DownloadSdkFix> e : files.entrySet()) {
      myNotifications.put(e.getKey().getSdkName(), SdkFixInfo.forFix(e));
    }

    for (FileEditor editor : myFileEditorManager.getAllEditors()) {
      updateEditorNotifications(editor);
    }
  }

  private void removeNotification(@NotNull MissingSdkNotificationPanel expiredPanel) {
    myNotifications.remove(expiredPanel.myInfo.getSdkName());

    for (FileEditor editor : myFileEditorManager.getAllEditors()) {
      List<MissingSdkNotificationPanel> notifications = editor.getUserData(NOTIFICATIONS);
      if (notifications == null) continue;

      for (MissingSdkNotificationPanel panel : new ArrayList<>(notifications)) {
        if (panel.myInfo.equals(expiredPanel.myInfo)) {
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
    }
    else {
      notifications = new SmartList<>();
      editor.putUserData(NOTIFICATIONS, notifications);
    }

    for (SdkFixInfo info : myNotifications.values()) {
      MissingSdkNotificationPanel notification = createPanelFor(info);

      notifications.add(notification);
      myFileEditorManager.addTopComponent(editor, notification);
    }
  }

  public static class MissingSdkNotificationPanel extends EditorNotificationPanel {
    private final SdkFixInfo myInfo;

    private MissingSdkNotificationPanel(@NotNull final SdkFixInfo info) {
      myInfo = info;
    }

    @NotNull
    public SdkFixInfo getInfo() {
      return myInfo;
    }
  }

  public static class SdkFixInfo {
    @NotNull
    static SdkFixInfo forBroken(@NotNull String sdkName) {
      return new SdkFixInfo(sdkName);
    }

    @NotNull
    static SdkFixInfo forFix(@NotNull Map.Entry<? extends UnknownSdk, DownloadSdkFix> e) {
      UnknownSdk sdk = e.getKey();
      DownloadSdkFix fix = e.getValue();
      return new SdkFixInfo(sdk.getSdkName()) {
        @NotNull
        @Override
        public Pair<UnknownSdk, DownloadSdkFix> getFixInfo() {
          return Pair.create(sdk, fix);
        }
      };
    }

    private final String mySdkName;

    private SdkFixInfo(@NotNull String sdkName) {
      mySdkName = sdkName;
    }

    @NotNull
    public String getSdkName() {
      return mySdkName;
    }

    @Nullable
    public Pair<UnknownSdk, DownloadSdkFix> getFixInfo() {
      return null;
    }

    @Override
    public final int hashCode() {
      return Objects.hashCode(mySdkName);
    }

    @Override
    public final boolean equals(Object obj) {
      return obj instanceof SdkFixInfo && Objects.equals(((SdkFixInfo)obj).mySdkName, mySdkName);
    }

    @Override
    public String toString() {
      return "SdkFixInfo{" +
             "mySdkName='" + mySdkName + '\'' +
             '}';
    }
  }
}
