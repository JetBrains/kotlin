// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.projectRoots.impl;

import com.intellij.notification.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.PerformInBackgroundOption;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.progress.util.ProgressIndicatorBase;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.ProjectJdkTable;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkModificator;
import com.intellij.openapi.projectRoots.SdkType;
import com.intellij.openapi.projectRoots.impl.SdkUsagesCollector.SdkUsage;
import com.intellij.openapi.projectRoots.impl.UnknownSdkResolver.DownloadSdkFix;
import com.intellij.openapi.projectRoots.impl.UnknownSdkResolver.LocalSdkFix;
import com.intellij.openapi.projectRoots.impl.UnknownSdkResolver.UnknownSdkLookup;
import com.intellij.openapi.roots.ui.configuration.ProjectSettingsService;
import com.intellij.openapi.roots.ui.configuration.projectRoot.SdkDownloadTask;
import com.intellij.openapi.roots.ui.configuration.projectRoot.SdkDownloadTracker;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.event.HyperlinkEvent;
import java.util.*;

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
      .run(new Task.Backgroundable(project, "Resolving SDKs", false, PerformInBackgroundOption.ALWAYS_BACKGROUND) {
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
      notifyDownloadableSdks(project, downloadFixes, () -> startDownloadingSdks(downloadFixes));
    });
  }

  private static void startDownloadingSdks(@NotNull Map<MissingSdkInfo, DownloadSdkFix> downloadFixes) {
    for (Map.Entry<MissingSdkInfo, DownloadSdkFix> entry : downloadFixes.entrySet()) {
      SdkDownloadTask task = entry.getValue().createTask(/*TODO: progress indicator!*/new ProgressIndicatorBase());
      MissingSdkInfo info = entry.getKey();
      Sdk sdk = createSdk(info);

      SdkModificator mod = sdk.getSdkModificator();
      mod.setHomePath(task.getPlannedHomeDir());
      mod.setVersionString(task.getPlannedVersion());

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
  }

  private static void configureLocalSdks(@NotNull Map<MissingSdkInfo, LocalSdkFix> localFixes) {
    ProjectJdkTable jdkTable = ProjectJdkTable.getInstance();
    for (Map.Entry<MissingSdkInfo, LocalSdkFix> e : localFixes.entrySet()) {
      MissingSdkInfo info = e.getKey();
      LocalSdkFix fix = e.getValue();

      Sdk sdk = jdkTable.createSdk(info.getSdkName(), info.getSdkType());
      SdkModificator mod = sdk.getSdkModificator();
      mod.setHomePath(FileUtil.toSystemIndependentName(fix.getExistingSdkHome()));
      mod.setVersionString(fix.getVersionString());
      mod.commitChanges();

      WriteAction.run(() -> jdkTable.addJdk(sdk));
      LOG.info("Automatically set Sdk " + info.getSdkName() + " to " + fix.getExistingSdkHome());
    }
  }

  @NotNull
  private static Sdk createSdk(@NotNull MissingSdkInfo info) {
    return ProjectJdkTable.getInstance().createSdk(info.getSdkName(), info.getSdkType());
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

  private static void notifyDownloadableSdks(@NotNull Project project,
                                             @NotNull Map<MissingSdkInfo, DownloadSdkFix> downloadFixes,
                                             @NotNull Runnable onDownloadApproved) {
    final String title;
    final StringBuilder message = new StringBuilder();

    if (downloadFixes.size() == 1) {
      Map.Entry<MissingSdkInfo, DownloadSdkFix> entry = downloadFixes.entrySet().iterator().next();
      MissingSdkInfo info = entry.getKey();
      DownloadSdkFix fix = entry.getValue();
      title = "Fix Missing " + info.getSdkType().getPresentableName();
      message.append("<a href='download'>");
      message.append("Download ").append(fix.getDownloadDescription());
      message.append("</a>");
    } else {
      title = "Fix Missing SDKs";
      message.append("<a href='download'>");
      message.append("Download all SDKs");
      message.append("</a>:<br/>");

      Set<String> usages = new TreeSet<>();
      for (Map.Entry<MissingSdkInfo, DownloadSdkFix> entry : downloadFixes.entrySet()) {
        DownloadSdkFix fix = entry.getValue();
        usages.add(fix.getDownloadDescription());
      }
      message.append(StringUtil.join(usages,"<br/>"));
    }

    message.append(" or <a href='tune'>Configure Manually...</a>");

    new NotificationGroup("Missing SDKs", NotificationDisplayType.BALLOON, true)
      .createNotification(title, message.toString(), NotificationType.INFORMATION,
                          new NotificationListener() {
                            @Override
                            public void hyperlinkUpdate(@NotNull final Notification notification,
                                                        @NotNull HyperlinkEvent event) {
                              if (event.getEventType() != HyperlinkEvent.EventType.ACTIVATED) return;
                              notification.expire();
                              if ("tune".equals(event.getDescription())) {
                                ProjectSettingsService.getInstance(project).openProjectSettings();
                              }
                              else if ("download".equals(event.getDescription())) {
                                ApplicationManager.getApplication().invokeLater(onDownloadApproved);
                              }
                            }
                          }).setImportant(true).notify(project);
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

    message.append(" or <a href='tune'>Configure Manually...</a>");
    SDK_CONFIGURED_GROUP.createNotification(title, message.toString(), NotificationType.INFORMATION, null)
      .setImportant(true)
      .addAction(NotificationAction.createSimple("Configure SDKs", () -> ProjectSettingsService.getInstance(project).openProjectSettings()))
      .notify(project);
  }

  @NotNull
  private static String getLocalFixPresentableHome(@NotNull LocalSdkFix fix) {
    //TODO: reuse with JdkListPresenter!
    String home = fix.getExistingSdkHome();
    home = StringUtil.trimEnd(home, "/Contents/Home");
    home = StringUtil.trimEnd(home, "/Contents/MacOS");
    home = StringUtil.shortenTextWithEllipsis(home, 50, 30);
    return home;
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
  }
}
