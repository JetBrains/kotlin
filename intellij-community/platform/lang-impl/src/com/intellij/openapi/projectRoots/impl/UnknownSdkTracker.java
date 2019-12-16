// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.projectRoots.impl;

import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.progress.util.ProgressIndicatorBase;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.*;
import com.intellij.openapi.projectRoots.impl.SdkUsagesCollector.SdkUsage;
import com.intellij.openapi.projectRoots.impl.UnknownSdkResolver.DownloadSdkFix;
import com.intellij.openapi.projectRoots.impl.UnknownSdkResolver.LocalSdkFix;
import com.intellij.openapi.projectRoots.impl.UnknownSdkResolver.UnknownSdkLookup;
import com.intellij.openapi.roots.ui.configuration.ProjectSettingsService;
import com.intellij.openapi.roots.ui.configuration.SdkListModelBuilder;
import com.intellij.openapi.roots.ui.configuration.SdkListPresenter;
import com.intellij.openapi.roots.ui.configuration.SdkPopupFactory;
import com.intellij.openapi.roots.ui.configuration.projectRoot.ProjectSdksModel;
import com.intellij.openapi.roots.ui.configuration.projectRoot.SdkDownloadTask;
import com.intellij.openapi.roots.ui.configuration.projectRoot.SdkDownloadTracker;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.EditorNotificationPanel;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.intellij.openapi.progress.PerformInBackgroundOption.ALWAYS_BACKGROUND;
import static com.intellij.openapi.projectRoots.impl.UnknownSdkResolver.EP_NAME;
import static com.intellij.openapi.projectRoots.impl.UnknownSdkResolver.UnknownSdk;

public class UnknownSdkTracker {
  private static final Logger LOG = Logger.getInstance(UnknownSdkTracker.class);

  public static class ActivityTracker implements StartupActivity.Background, StartupActivity.DumbAware {
    @Override
    public void runActivity(@NotNull Project project) {
      StartupManager.getInstance(project).runWhenProjectIsInitialized(() -> updateUnknownSdks(project));
    }
  }

  private static void updateUnknownSdks(@NotNull Project project) {
    if (!EP_NAME.hasAnyExtensions()) return;

    ProgressManager.getInstance()
      .run(new Task.Backgroundable(project, "Resolving SDKs", false, ALWAYS_BACKGROUND) {
        @Override
        public void run(@NotNull ProgressIndicator indicator) {
          List<UnknownSdkLookup> lookups = new ArrayList<>();
          EP_NAME.forEachExtensionSafe(ext -> {
            UnknownSdkLookup resolver = ext.createResolver(project, indicator);
            if (resolver != null) {
              lookups.add(resolver);
            }
          });

          if (lookups.isEmpty()) return;
          updateUnknownSdksWithProgress(project, indicator, lookups);
        }
      });
  }

  private static void updateUnknownSdksWithProgress(@NotNull Project project,
                                                    @NotNull ProgressIndicator indicator,
                                                    @NotNull List<UnknownSdkLookup> lookups) {
    List<MissingSdkInfo> fixable = ReadAction.compute(() -> collectUnknownSdks(project));
    if (fixable.isEmpty()) return;

    Map<MissingSdkInfo, LocalSdkFix> localFixes = findLocalFixes(indicator, fixable, lookups);
    Map<MissingSdkInfo, DownloadSdkFix> downloadFixes = findDownloadFixes(indicator,
                                                                          ContainerUtil.filter(fixable, info -> !localFixes.containsKey(info)),
                                                                          lookups);

    if (localFixes.isEmpty() && downloadFixes.isEmpty()) return;

    ApplicationManager.getApplication().invokeLater(() -> {
      configureLocalSdks(localFixes);

      notifyFixedSdks(project, localFixes);

      if (!downloadFixes.isEmpty()) {
        GlobalEditorNotification.getInstance(project).showNotifications(downloadFixes);
      }
    });
  }

  private static void applyDownloadableFix(@NotNull Project project,
                                           @NotNull MissingSdkInfo info,
                                           @NotNull DownloadSdkFix fix) {
    SdkDownloadTask task;
    String title = "Configuring SDK";
    try {
      task = ProgressManager.getInstance().run(new Task.WithResult<SdkDownloadTask, RuntimeException>(project, title, true) {
        @Override
        protected SdkDownloadTask compute(@NotNull ProgressIndicator indicator) {
          return fix.createTask(indicator);
        }
      });
    } catch (ProcessCanceledException e) {
      throw e;
    } catch (Exception error) {
      LOG.warn("Failed to download " + info.getSdkType().getPresentableName() + " " + fix.getDownloadDescription() + " for " + info.getSdkName() + ". " + error.getMessage(), error);
      Messages.showErrorDialog("Failed to download " + fix.getDownloadDescription() + ". " + error.getMessage(), title);
      return;
    }

    Sdk sdk = ProjectJdkTable.getInstance().createSdk(info.getSdkName(), info.getSdkType());
    {
      SdkModificator mod = sdk.getSdkModificator();
      mod.setHomePath(task.getPlannedHomeDir());
      mod.setVersionString(task.getPlannedVersion());
      mod.commitChanges();
    }

    ApplicationManager.getApplication().invokeLater(() -> {
      SdkDownloadTracker downloadTracker = SdkDownloadTracker.getInstance();
      downloadTracker.registerSdkDownload(sdk, task);
      downloadTracker.tryRegisterDownloadingListener(sdk, Disposer.newDisposable(), new ProgressIndicatorBase(), succeeded -> {
        if (succeeded) {
          WriteAction.run(() -> ProjectJdkTable.getInstance().addJdk(sdk));
        }
      });
      downloadTracker.startSdkDownloadIfNeeded(sdk);
    });
  }

  private static void configureLocalSdks(@NotNull Map<MissingSdkInfo, LocalSdkFix> localFixes) {
    ProjectJdkTable jdkTable = ProjectJdkTable.getInstance();
    for (Map.Entry<MissingSdkInfo, LocalSdkFix> e : localFixes.entrySet()) {
      MissingSdkInfo info = e.getKey();
      LocalSdkFix fix = e.getValue();

      Sdk sdk = jdkTable.createSdk(info.getSdkName(), info.getSdkType());
      {
        SdkModificator mod = sdk.getSdkModificator();
        mod.setHomePath(FileUtil.toSystemIndependentName(fix.getExistingSdkHome()));
        mod.setVersionString(fix.getVersionString());
        mod.commitChanges();
      }

      WriteAction.run(() -> jdkTable.addJdk(sdk));
      LOG.info("Automatically set Sdk " + info.getSdkName() + " to " + fix.getExistingSdkHome());
    }
  }

  @NotNull
  private static Map<MissingSdkInfo, LocalSdkFix> findLocalFixes(@NotNull ProgressIndicator indicator,
                                                                 @NotNull List<MissingSdkInfo> infos,
                                                                 @NotNull List<UnknownSdkLookup> lookups) {

    Map<MissingSdkInfo, LocalSdkFix> result = new LinkedHashMap<>();
    for (MissingSdkInfo info : infos) {
      for (UnknownSdkLookup lookup : lookups) {
        LocalSdkFix fix = lookup.proposeLocalFix(info, indicator);
        if (fix == null) continue;
        result.put(info, fix);
      }
    }
    return result;
  }

  @NotNull
  private static Map<MissingSdkInfo, DownloadSdkFix> findDownloadFixes(@NotNull ProgressIndicator indicator,
                                                                       @NotNull List<MissingSdkInfo> infos,
                                                                       @NotNull List<UnknownSdkLookup> lookups) {

    Map<MissingSdkInfo, DownloadSdkFix> result = new LinkedHashMap<>();
    for (MissingSdkInfo info : infos) {
      for (UnknownSdkLookup lookup : lookups) {
        DownloadSdkFix fix = lookup.proposeDownload(info, indicator);
        if (fix == null) continue;
        result.put(info, fix);
      }
    }
    return result;
  }

  @NotNull
  private static List<MissingSdkInfo> collectUnknownSdks(@NotNull Project project) {
    Map<String, MissingSdkInfo> myInfos = new HashMap<>();
    List<SdkUsage> usages = SdkUsagesCollector.getInstance(project).collectSdkUsages(project);
    ProjectJdkTable jdkTable = ProjectJdkTable.getInstance();
    for (SdkUsage usage : usages) {
      String sdkName = usage.getSdkName();

      //we do not track existing SDKs
      if (jdkTable.findJdk(sdkName) != null) continue;

      MissingSdkInfo info = myInfos.get(sdkName);
      if (info == null) {
        info = new MissingSdkInfo(sdkName);
        myInfos.put(sdkName, info);
      }

      info.attachSdkType(usage.getSdkTypeName());
    }

    return ContainerUtil.filter(myInfos.values(), sdk -> sdk.mySdkType != null);
  }

  private static final NotificationGroup SDK_CONFIGURED_GROUP
    = new NotificationGroup("Missing SDKs", NotificationDisplayType.BALLOON, true);

  private static void notifyFixedSdks(@NotNull Project project,
                                      @NotNull Map<MissingSdkInfo, LocalSdkFix> localFixes) {
    final String title;
    final StringBuilder message = new StringBuilder();
    if (localFixes.isEmpty()) return;

    Set<String> usages = new TreeSet<>();
    for (Map.Entry<MissingSdkInfo, LocalSdkFix> entry : localFixes.entrySet()) {
      LocalSdkFix fix = entry.getValue();
      String usage = "\"" + entry.getKey().getSdkName() + "\"" +
                       " is set to " +
                       fix.getVersionString() +
                       " <br/> " +
                       getLocalFixPresentableHome(fix);
      usages.add(usage);
    }
    message.append(StringUtil.join(usages, "<br/><br/>"));

    if (localFixes.size() == 1) {
      Map.Entry<MissingSdkInfo, LocalSdkFix> entry = localFixes.entrySet().iterator().next();
      MissingSdkInfo info = entry.getKey();
      title = info.getSdkType().getPresentableName() + " is configured";
    } else {
      title = "SDKs are configured";
    }

    SDK_CONFIGURED_GROUP.createNotification(title, message.toString(), NotificationType.INFORMATION, null)
      .setImportant(true)
      .addAction(NotificationAction.createSimple("Change in the Project Structure Dialog...", () -> ProjectSettingsService.getInstance(project).openProjectSettings()))
      .notify(project);
  }

  @NotNull
  private static String getLocalFixPresentableHome(@NotNull LocalSdkFix fix) {
    String home = fix.getExistingSdkHome();
    return SdkListPresenter.presentDetectedSdkPath(home);
  }

  private static class MissingSdkInfo implements UnknownSdk {
    @NotNull private final String mySdkName;
    @NotNull private final Set<String> mySdkTypeNames = new HashSet<>(); // nullable keys are ok
    @Nullable private SdkType mySdkType;

    MissingSdkInfo(@NotNull String sdkName) {
      mySdkName = sdkName;
    }

    void attachSdkType(@Nullable String name) {
      if (!mySdkTypeNames.add(name)) return; //null is ok here

      if (name == null || mySdkTypeNames.size() != 1) {
        mySdkType = null;
      }
      else {
        for (SdkType type : SdkType.getAllTypes()) {
          if (type.getName().equals(name) && type.allowCreationByUser()) {
            mySdkType = type;
          }
        }
      }
    }

    @NotNull
    @Override
    public String getSdkName() {
      return mySdkName;
    }

    @NotNull
    @Override
    public SdkType getSdkType() {
      assert mySdkType != null;
      return mySdkType;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof MissingSdkInfo)) return false;
      MissingSdkInfo info = (MissingSdkInfo)o;
      return mySdkName.equals(info.mySdkName);
    }

    @Override
    public int hashCode() {
      return Objects.hash(mySdkName);
    }
  }

  static class GlobalEditorNotification implements Disposable {
    private final Key<List<MissingSdkNotificationPanel>> NOTIFICATIONS_ADDED = Key.create("notifications added to the editor");
    private final Key<?> myEditorNotificationKeyForFus = Key.create("fix project SDK");

    @NotNull
    public static GlobalEditorNotification getInstance(@NotNull Project project) {
      return project.getService(GlobalEditorNotification.class);
    }

    private final Project myProject;
    private final FileEditorManager myFileEditorManager;
    private Map<MissingSdkInfo, DownloadSdkFix> myNotifications = new HashMap<>();

    GlobalEditorNotification(@NotNull Project project) {
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
      private final MissingSdkInfo myInfo;

      private MissingSdkNotificationPanel(@NotNull final MissingSdkInfo info) {
        myInfo = info;
      }

      public boolean isSameProblemAs(@NotNull MissingSdkNotificationPanel panel) {
        return this.myInfo.equals(panel.myInfo);
      }
    }

    @NotNull
    private MissingSdkNotificationPanel createPanelFor(@NotNull MissingSdkInfo info,
                                                       @NotNull DownloadSdkFix fix) {
      String sdkName = info.getSdkType().getPresentableName();

      MissingSdkNotificationPanel panel = new MissingSdkNotificationPanel(info);
      panel.setProject(myProject);
      panel.setProviderKey(myEditorNotificationKeyForFus);
      panel.setText(sdkName + " \"" + info.getSdkName() + "\" is missing");

      panel.createActionLabel("Download " + sdkName + " (" + fix.getDownloadDescription() + ")", () -> {
        removeNotification(panel);
        applyDownloadableFix(myProject, info, fix);
      });

      panel.createActionLabel("Configure...", () -> {}).addHyperlinkListener(new HyperlinkListener() {
        @Override
        public void hyperlinkUpdate(HyperlinkEvent e) {
          ProjectSdksModel model = new ProjectSdksModel();

          SdkListModelBuilder modelBuilder = new SdkListModelBuilder(
            myProject,
            model,
            sdkType -> Objects.equals(sdkType, info.getSdkType()),
            null,
            null);

          SdkPopupFactory popup = new SdkPopupFactory(
            myProject,
            model,
            modelBuilder
          );

          AtomicBoolean wasSdkCreated = new AtomicBoolean(false);

          model.addListener(new SdkModel.Listener() {
            @Override
            public void sdkAdded(@NotNull Sdk sdk) {
              //it is easier and safer than committing the ProjectSdksModel instance
              WriteAction.run(() -> {
                SdkModificator mod = sdk.getSdkModificator();
                mod.setName(info.getSdkName());
                mod.commitChanges();

                ProjectJdkTable.getInstance().addJdk(sdk);
                wasSdkCreated.set(true);
              });
            }
          });

          //FileEditorManager#addTopComponent wraps the panel to implement borders, unwrapping
          Container container = panel.getParent();
          if (container == null) container = panel;
          popup.showUnderneathToTheRightOf(
            container,
            () -> {
              if (wasSdkCreated.get()) {
                removeNotification(panel);
              }
            }
          );
        }
      });
      return panel;
    }

    @Override
    public void dispose() { }

    public void showNotifications(@NotNull final Map<MissingSdkInfo, DownloadSdkFix> files) {
      myNotifications = new HashMap<>(files);

      for (FileEditor editor : myFileEditorManager.getAllEditors()) {
        updateEditorNotifications(editor);
      }
    }

    private void removeNotification(@NotNull MissingSdkNotificationPanel expiredPanel) {
      myNotifications.remove(expiredPanel.myInfo);
      for (FileEditor editor : myFileEditorManager.getAllEditors()) {
        List<MissingSdkNotificationPanel> notifications = editor.getUserData(NOTIFICATIONS_ADDED);
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

      List<MissingSdkNotificationPanel> notifications = editor.getUserData(NOTIFICATIONS_ADDED);
      if (notifications != null) {
        for (JComponent component : notifications) {
          myFileEditorManager.removeTopComponent(editor, component);
        }
        notifications.clear();
      } else {
        notifications = new SmartList<>();
        editor.putUserData(NOTIFICATIONS_ADDED, notifications);
      }

      for (Map.Entry<MissingSdkInfo, DownloadSdkFix> e : myNotifications.entrySet()) {
        MissingSdkNotificationPanel notification = createPanelFor(e.getKey(), e.getValue());

        notifications.add(notification);
        myFileEditorManager.addTopComponent(editor, notification);
      }
    }
  }
}
