// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.projectRoots.impl;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.intellij.codeInsight.intention.PriorityAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectBundle;
import com.intellij.openapi.projectRoots.SdkType;
import com.intellij.openapi.roots.ui.configuration.SdkListPresenter;
import com.intellij.openapi.roots.ui.configuration.UnknownSdk;
import com.intellij.openapi.roots.ui.configuration.UnknownSdkDownloadableSdkFix;
import com.intellij.openapi.roots.ui.configuration.UnknownSdkLocalSdkFix;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.EditorNotificationPanel;
import com.intellij.ui.EditorNotifications;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.util.SmartList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class UnknownSdkEditorNotification {
  public static final Key<List<EditorNotificationPanel>> NOTIFICATIONS = Key.create("notifications added to the editor");
  private static final Key<?> EDITOR_NOTIFICATIONS_KEY = Key.create("SdkSetupNotificationNew");

  @NotNull
  public static UnknownSdkEditorNotification getInstance(@NotNull Project project) {
    return project.getService(UnknownSdkEditorNotification.class);
  }

  private final Project myProject;
  private final FileEditorManager myFileEditorManager;
  private final AtomicReference<Set<SimpleSdkFixInfo>> myNotifications = new AtomicReference<>(new LinkedHashSet<>());

  UnknownSdkEditorNotification(@NotNull Project project) {
    myProject = project;
    myFileEditorManager = FileEditorManager.getInstance(myProject);
    myProject.getMessageBus()
      .connect()
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
    return myNotifications.get().isEmpty();
  }

  @NotNull
  public List<SimpleSdkFixInfo> getNotifications() {
    return ImmutableList.copyOf(myNotifications.get());
  }

  public void showNotifications(@NotNull List<UnknownSdk> unfixableSdks,
                                @NotNull Map<UnknownSdk, UnknownSdkDownloadableSdkFix> files,
                                @NotNull List<UnknownInvalidSdk> invalidSdks) {
    ImmutableSet.Builder<SimpleSdkFixInfo> notifications = ImmutableSet.builder();

    if (Registry.is("unknown.sdk.show.editor.actions")) {
      for (UnknownSdk e : unfixableSdks) {
        @Nullable String name = e.getSdkName();
        SdkType type = e.getSdkType();
        if (name == null) continue;
        notifications.add(new UnknownSdkFixInfo(name, type, null, null));
      }

      for (Map.Entry<UnknownSdk, UnknownSdkDownloadableSdkFix> e : files.entrySet()) {
        UnknownSdk unknownSdk = e.getKey();
        String name = unknownSdk.getSdkName();
        if (name == null) continue;

        UnknownSdkDownloadableSdkFix fix = e.getValue();
        notifications.add(new UnknownSdkFixInfo(name, unknownSdk.getSdkType(), unknownSdk, fix));
      }

      for (UnknownInvalidSdk sdk : invalidSdks) {
        notifications.add(new InvalidSdkFixInfo(sdk));
      }
    }

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

    for (SimpleSdkFixInfo info : myNotifications.get()) {
      VirtualFile file = editor.getFile();
      if (file == null) continue;

      EditorNotificationPanel notification = info.createNotificationPanel(file, myProject);
      if (notification == null) continue;

      notifications.add(notification);
      myFileEditorManager.addTopComponent(editor, notification);
    }
  }

  public abstract static class SimpleSdkFixInfo {
    @NotNull protected final SdkType mySdkType;

    protected SimpleSdkFixInfo(@NotNull SdkType sdkType) {
      mySdkType = sdkType;
    }

    @Nullable
    final EditorNotificationPanel createNotificationPanel(@NotNull VirtualFile file, @NotNull Project project) {
      //we must not show the notification for an unrelevant files in the project
      if (!mySdkType.isRelevantForFile(project, file)) {
        return null;
      }

      return createNotificationPanelImpl(file, project);
    }

    @NotNull
    abstract EditorNotificationPanel createNotificationPanelImpl(@NotNull VirtualFile file, @NotNull Project project);

    @NotNull
    protected EditorNotificationPanel newNotificationPanel(@NotNull String intentionActionText) {
      return new EditorNotificationPanel() {
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
          return ProjectBundle.message("config.unknown.sdk.configuration");
        }
      };
    }
  }

  private class UnknownSdkFixInfo extends SimpleSdkFixInfo {
    @NotNull private final String mySdkName;
    @Nullable private final UnknownSdk mySdk;
    @Nullable private final UnknownSdkDownloadableSdkFix myFix;

    UnknownSdkFixInfo(@NotNull String sdkName,
                      @NotNull SdkType sdkType,
                      @Nullable UnknownSdk sdk,
                      @Nullable UnknownSdkDownloadableSdkFix fix) {
      super(sdkType);
      mySdkName = sdkName;
      mySdk = sdk;
      myFix = fix;
    }

    @NotNull
    @Override
    final EditorNotificationPanel createNotificationPanelImpl(@NotNull VirtualFile file, @NotNull Project project) {
      String sdkTypeName = mySdkType.getPresentableName();
      String notificationText = ProjectBundle.message("config.unknown.sdk.notification.text", sdkTypeName, mySdkName);
      String configureText = ProjectBundle.message("config.unknown.sdk.configure");

      boolean hasDownload = myFix != null && mySdk != null;
      String downloadText = hasDownload ? ProjectBundle.message("config.unknown.sdk.download", myFix.getDownloadDescription()) : "";
      String intentionActionText =
        hasDownload ? downloadText : ProjectBundle.message("config.unknown.sdk.configure.missing", sdkTypeName, mySdkName);

      EditorNotificationPanel notification = newNotificationPanel(intentionActionText);

      notification.setProject(myProject);
      notification.setProviderKey(EDITOR_NOTIFICATIONS_KEY);
      notification.setText(notificationText);

      if (hasDownload) {
        AtomicBoolean isRunning = new AtomicBoolean(false);
        notification.createActionLabel(downloadText, () -> {
          if (isRunning.compareAndSet(false, true)) {
            UnknownSdkTracker.getInstance(myProject).applyDownloadableFix(mySdk, myFix);
          }
        }, true);
      }

      notification.createActionLabel(configureText,
                                     UnknownSdkTracker
                                       .getInstance(myProject)
                                       .createSdkSelectionPopup(mySdkName, mySdkType),
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

  private class InvalidSdkFixInfo extends SimpleSdkFixInfo {
    @NotNull private final String mySdkName;
    @NotNull private final UnknownInvalidSdk mySdk;

    InvalidSdkFixInfo(@NotNull UnknownInvalidSdk invalidSdk) {
      super(invalidSdk.mySdkType);
      mySdkName = invalidSdk.getSdkName();
      mySdk = invalidSdk;
    }

    @NotNull
    @Override
    final EditorNotificationPanel createNotificationPanelImpl(@NotNull VirtualFile file, @NotNull Project project) {
      String sdkTypeName = mySdkType.getPresentableName();
      String notificationText = ProjectBundle.message("config.invalid.sdk.notification.text", sdkTypeName, mySdkName);
      String configureText = ProjectBundle.message("config.invalid.sdk.configure");

      UnknownSdkLocalSdkFix localFix = mySdk.myLocalSdkFix;
      UnknownSdkDownloadableSdkFix downloadFix = mySdk.myDownloadableSdkFix;

      String intentionActionText = ProjectBundle.message("config.invalid.sdk.configure.missing", sdkTypeName, mySdkName);

      String localText = "";
      String localTextTooltip = "";
      if (localFix != null) {
        localText = intentionActionText = ProjectBundle.message("config.unknown.sdk.local", sdkTypeName, localFix.getPresentableVersionString());
        localTextTooltip = SdkListPresenter.presentDetectedSdkPath(localFix.getExistingSdkHome(), 90, 40);
      }

      String downloadText = downloadFix != null ? intentionActionText = ProjectBundle.message("config.unknown.sdk.download", downloadFix.getDownloadDescription()) : "";
      EditorNotificationPanel notification = newNotificationPanel(intentionActionText);

      notification.setProject(myProject);
      notification.setProviderKey(EDITOR_NOTIFICATIONS_KEY);
      notification.setText(notificationText);

      if (localFix != null) {
        HyperlinkLabel actionLabel = notification.createActionLabel(localText, () -> {
          mySdk.applyLocalFix(project);
        }, true);
        actionLabel.setToolTipText(localTextTooltip);
      }
      else if (downloadFix != null) {
        notification.createActionLabel(downloadText, () -> {
          mySdk.applyDownloadFix(myProject);
        }, true);
      }

      notification.createActionLabel(configureText,
                                     mySdk.createSdkSelectionPopup(project),
                                     true
      );

      return notification;
    }

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("InvalidSdkFixInfo { name: ").append(mySdkName);
      if (mySdk.myLocalSdkFix != null) {
        sb.append(", fix: ").append(mySdk.myLocalSdkFix.getExistingSdkHome());
      }
      if (mySdk.myDownloadableSdkFix != null) {
        sb.append(", fix: ").append(mySdk.myDownloadableSdkFix.getDownloadDescription());
      }
      sb.append("}");
      return sb.toString();
    }
  }
}
