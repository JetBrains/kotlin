// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.projectRoots.impl;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.intellij.codeInsight.intention.PriorityAction;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.SdkType;
import com.intellij.openapi.projectRoots.impl.UnknownSdkResolver.DownloadSdkFix;
import com.intellij.openapi.projectRoots.impl.UnknownSdkResolver.UnknownSdk;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.EditorNotificationPanel;
import com.intellij.ui.EditorNotifications;
import com.intellij.util.SmartList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class UnknownSdkEditorNotification implements Disposable {
  public static final Key<List<EditorNotificationPanel>> NOTIFICATIONS = Key.create("notifications added to the editor");
  private static final Key<?> EDITOR_NOTIFICATIONS_KEY = Key.create("SdkSetupNotificationNew");

  @NotNull
  public static UnknownSdkEditorNotification getInstance(@NotNull Project project) {
    return project.getService(UnknownSdkEditorNotification.class);
  }

  private final Project myProject;
  private final FileEditorManager myFileEditorManager;
  private final AtomicBoolean myAllowProjectSdkNotifications = new AtomicBoolean(true);
  private final AtomicReference<Set<SdkFixInfo>> myNotifications = new AtomicReference<>(new LinkedHashSet<>());

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

  public boolean allowProjectSdkNotifications() {
    return myAllowProjectSdkNotifications.get();
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

  @Override
  public void dispose() { }

  @NotNull
  public List<SdkFixInfo> getNotifications() {
    return ImmutableList.copyOf(myNotifications.get());
  }

  public void showNotifications(boolean allowProjectSdkValidation,
                                @NotNull Map<String, SdkType> unifiableSdkNames,
                                @NotNull Map<UnknownSdk, DownloadSdkFix> files) {
    ImmutableSet.Builder<SdkFixInfo> notifications = ImmutableSet.builder();
    for (Map.Entry<String, SdkType> e : unifiableSdkNames.entrySet()) {
      String name = e.getKey();
      @Nullable SdkType type = e.getValue();
      notifications.add(new SimpleSdkFixInfo(name, type));
    }

    for (Map.Entry<UnknownSdk, DownloadSdkFix> e : files.entrySet()) {
      notifications.add(new SimpleSdkFixInfo(e.getKey(), e.getValue()));
    }

    myAllowProjectSdkNotifications.getAndSet(allowProjectSdkValidation);
    myNotifications.set(notifications.build());
    EditorNotifications.getInstance(myProject).updateAllNotifications();

    ApplicationManager.getApplication().invokeLater(() -> {
      for (FileEditor editor : myFileEditorManager.getAllEditors()) {
        updateEditorNotifications(editor);
      }
    });
  }

  private void updateEditorNotifications(@NotNull FileEditor editor) {
    if (!editor.isValid()) return;

    List<EditorNotificationPanel> notifications = editor.getUserData(NOTIFICATIONS);
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

    for (SdkFixInfo info : myNotifications.get()) {
      VirtualFile file = editor.getFile();
      if (file == null) continue;

      EditorNotificationPanel notification = info.createNotificationPanel(file, editor, myProject);
      if (notification == null) continue;

      notifications.add(notification);
      myFileEditorManager.addTopComponent(editor, notification);
    }
  }

  public interface SdkFixInfo {
    @Nullable
    EditorNotificationPanel createNotificationPanel(@NotNull VirtualFile file,
                                                    @NotNull FileEditor fileEditor,
                                                    @NotNull Project project);
  }

  private class SimpleSdkFixInfo implements SdkFixInfo {
    private final String mySdkName;
    @Nullable private final SdkType mySdkType;
    @Nullable private final UnknownSdk mySdk;
    @Nullable private final DownloadSdkFix myFix;

    SimpleSdkFixInfo(String sdkName, @Nullable SdkType sdkType) {
      mySdkName = sdkName;
      mySdkType = sdkType;
      mySdk = null;
      myFix = null;
    }

    SimpleSdkFixInfo(@NotNull UnknownSdk sdk, @NotNull DownloadSdkFix fix) {
      mySdkName = sdk.getSdkName();
      mySdkType = sdk.getSdkType();
      mySdk = sdk;
      myFix = fix;
    }

    @NotNull
    @Override
    public final EditorNotificationPanel createNotificationPanel(@NotNull VirtualFile file,
                                                                 @NotNull FileEditor fileEditor,
                                                                 @NotNull Project project) {
      String sdkTypeName = mySdkType != null ? mySdkType.getPresentableName() : "SDK";
      String quotedSdkName = "\"" + mySdkName + "\"";
      String notificationText = sdkTypeName + " " + quotedSdkName + " is missing";
      String configureText = "Configure...";

      boolean hasDownload = myFix != null && mySdk != null;
      String downloadText = hasDownload ? "Download " + myFix.getDownloadDescription() : "";
      String intentionActionText = hasDownload ? downloadText : "Configure missing " + sdkTypeName + " " + quotedSdkName;

      EditorNotificationPanel notification = new EditorNotificationPanel() {
        @Override
        protected String getIntentionActionText() {
          return intentionActionText;
        }

        @NotNull
        @Override
        protected PriorityAction.Priority getIntentionActionPriority() {
          return PriorityAction.Priority.HIGH;
        }

        @NotNull
        @Override
        protected String getIntentionActionFamilyName() {
          return "SDK configuration";
        }
      };

      notification.setProject(myProject);
      notification.setProviderKey(EDITOR_NOTIFICATIONS_KEY);
      notification.setText(notificationText);

      if (hasDownload) {
        notification.createActionLabel(downloadText, () -> {
          UnknownSdkTracker.getInstance(myProject).applyDownloadableFix(mySdk, myFix);
        }, true);
      }

      notification.createActionLabel(configureText,
                                     () -> {
                                       UnknownSdkTracker
                                         .getInstance(myProject)
                                         .showSdkSelectionPopup(mySdkName, mySdkType, parentJComponentOrSelf(notification));
                                     },
                                     true
      );

      return notification;
    }

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("SdkFixInfo { name: ").append(mySdkName);
      if (myFix != null) {
        sb.append(", fix: ").append(myFix.getDownloadDescription());
      }
      sb.append("}");
      return sb.toString();
    }
  }
}
