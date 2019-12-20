// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.projectRoots.impl;

import com.google.common.collect.ImmutableList;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkType;
import com.intellij.openapi.projectRoots.impl.SdkUsagesCollector.SdkUsage;
import com.intellij.openapi.projectRoots.impl.UnknownSdkResolver.DownloadSdkFix;
import com.intellij.openapi.projectRoots.impl.UnknownSdkResolver.UnknownSdk;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.EditorNotificationPanel;
import com.intellij.util.SmartList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.function.Consumer;

public class UnknownSdkEditorNotification implements Disposable {
  public static final Key<List<MissingSdkNotificationPanel>> NOTIFICATIONS = Key.create("notifications added to the editor");
  private static final Key<?> EDITOR_NOTIFICATIONS_KEY = Key.create("SdkSetupNotificationNew");

  @NotNull
  public static UnknownSdkEditorNotification getInstance(@NotNull Project project) {
    return project.getService(UnknownSdkEditorNotification.class);
  }

  private final Project myProject;
  private final FileEditorManager myFileEditorManager;
  private final Set<SdkFixInfo> myNotifications = new LinkedHashSet<>();

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

  private void setupPanel(@NotNull MissingSdkNotificationPanel panel,
                          @NotNull String sdkName,
                          @Nullable UnknownSdk unknownSdk,
                          @Nullable DownloadSdkFix fix) {

    SdkType sdkType = unknownSdk != null ? unknownSdk.getSdkType() : null;
    String sdkTypeName = sdkType != null ? sdkType.getPresentableName() : "SDK";

    panel.setText(sdkTypeName + " \"" + sdkName + "\" is missing");

    if (fix != null && unknownSdk != null) {
      panel.createActionLabel("Download " + fix.getDownloadDescription(), () -> {
        removeNotification(panel);
        UnknownSdkTracker.getInstance(myProject).applyDownloadableFix(unknownSdk, fix);
      });
    }

    panel.createActionLabel("Configure...",
                            () -> {
                              UnknownSdkTracker
                                .getInstance(myProject)
                                .showSdkSelectionPopup(sdkName, sdkType, parentJComponentOrSelf(panel), sdk -> removeNotification(panel));
                            }
    );
  }

  @NotNull
  private static JComponent parentJComponentOrSelf(@NotNull JComponent panel) {
    //FileEditorManager#addTopComponent wraps the panel to implement borders, unwrapping
    Container parent = panel.getParent();
    if (parent instanceof JComponent) {
      return (JComponent)parent;
    }
    return panel;
  }

  private void setupPanel(@NotNull MissingSdkNotificationPanel panel,
                          @NotNull String source,
                          @Nullable Runnable setProjectSdk,
                          @Nullable Consumer<Sdk> setSdk) {

    panel.setProviderKey(EDITOR_NOTIFICATIONS_KEY);
    panel.setText("SDK is not set for " + source);

    if (setProjectSdk != null) {
      panel.createActionLabel("Use Project SDK", () -> {
        setProjectSdk.run();
        removeNotification(panel);
      });
    }

    if (setSdk != null) {
      panel.createActionLabel("Configure...", () -> {
        UnknownSdkTracker
          .getInstance(myProject)
          .showSdkSelectionPopup(null, null, parentJComponentOrSelf(panel), sdk -> {
            setSdk.accept(sdk);
            removeNotification(panel);
          });
      });
    }
  }

  @Override
  public void dispose() { }

  @NotNull
  public List<SdkFixInfo> getNotifications() {
    return ImmutableList.copyOf(myNotifications);
  }

  public void showNotifications(@NotNull List<SdkUsage> unsetSdks,
                                @NotNull List<String> unifiableSdkNames,
                                @NotNull Map<UnknownSdk, DownloadSdkFix> files) {
    if (!Registry.is("sdk.auto.use.editor.notification")) return;

    myNotifications.clear();

    if (ApplicationManager.getApplication().isUnitTestMode() || Registry.is("sdk.auto.use.editor.notification.for.unset")) {
      for (SdkUsage usage : unsetSdks) {
        myNotifications.add(new SdkFixInfo() {
          @Override
          public void setupNotificationPanel(@NotNull MissingSdkNotificationPanel panel) {
            setupPanel(panel, usage.getUsagePresentableText(), usage.getProjectSdkSetAction(), usage.getSdkSetAction());
          }

          @Override
          public String toString() {
            return "SdkFixInfo { sdkUsage: " + usage.getUsagePresentableText() + " }";
          }
        });
      }
    }

    for (String name : unifiableSdkNames) {
      myNotifications.add(new SdkFixInfo() {
        @Override
        public void setupNotificationPanel(@NotNull MissingSdkNotificationPanel panel) {
          setupPanel(panel, name, (UnknownSdk)null, null);
        }

        @Override
        public String toString() {
          return "SdkFixInfo { unknownName: '" + name + "' }";
        }
      });
    }

    for (Map.Entry<UnknownSdk, DownloadSdkFix> e : files.entrySet()) {
      UnknownSdk key = e.getKey();
      DownloadSdkFix fix = e.getValue();
      myNotifications.add(new SdkFixInfo() {
        @Override
        public void setupNotificationPanel(@NotNull MissingSdkNotificationPanel panel) {
          setupPanel(panel, key.getSdkName(), key, fix);
        }

        @Override
        public String toString() {
          return "SdkFixInfo { name: '" + key.getSdkName() + "', fix: " + fix.getDownloadDescription() + " }";
        }
      });
    }

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

    for (SdkFixInfo info : myNotifications) {
      MissingSdkNotificationPanel notification = new MissingSdkNotificationPanel(info);
      notification.setProject(myProject);
      notification.setProviderKey(EDITOR_NOTIFICATIONS_KEY);
      info.setupNotificationPanel(notification);

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

  public interface SdkFixInfo {
    void setupNotificationPanel(@NotNull MissingSdkNotificationPanel panel);
  }
}
