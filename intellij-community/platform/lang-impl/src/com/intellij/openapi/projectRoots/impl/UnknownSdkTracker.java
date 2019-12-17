// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.projectRoots.impl;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.diagnostic.Logger;
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
import com.intellij.openapi.roots.ui.configuration.SdkListModelBuilder;
import com.intellij.openapi.roots.ui.configuration.SdkPopupFactory;
import com.intellij.openapi.roots.ui.configuration.projectRoot.ProjectSdksModel;
import com.intellij.openapi.roots.ui.configuration.projectRoot.SdkDownloadTask;
import com.intellij.openapi.roots.ui.configuration.projectRoot.SdkDownloadTracker;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.intellij.openapi.progress.PerformInBackgroundOption.ALWAYS_BACKGROUND;
import static com.intellij.openapi.projectRoots.impl.UnknownSdkResolver.UnknownSdk;

public class UnknownSdkTracker {
  private static final Logger LOG = Logger.getInstance(UnknownSdkTracker.class);

  @NotNull
  public static UnknownSdkTracker getInstance(@NotNull Project project) {
    return project.getService(UnknownSdkTracker.class);
  }

  @NotNull private final Project myProject;

  public UnknownSdkTracker(@NotNull Project project) {
    myProject = project;
  }

  public void updateUnknownSdks() {
    if (!UnknownSdkResolver.EP_NAME.hasAnyExtensions()) return;

    ProgressManager.getInstance()
      .run(new Task.Backgroundable(myProject, "Resolving SDKs", false, ALWAYS_BACKGROUND) {
        @Override
        public void run(@NotNull ProgressIndicator indicator) {
          List<UnknownSdkLookup> lookups = new ArrayList<>();
          UnknownSdkResolver.EP_NAME.forEachExtensionSafe(ext -> {
            UnknownSdkLookup resolver = ext.createResolver(UnknownSdkTracker.this.myProject, indicator);
            if (resolver != null) {
              lookups.add(resolver);
            }
          });

          if (lookups.isEmpty()) return;
          updateUnknownSdksWithProgress(indicator, lookups);
        }
      });
  }

  private void updateUnknownSdksWithProgress(@NotNull ProgressIndicator indicator,
                                             @NotNull List<UnknownSdkLookup> lookups) {
    List<MissingSdkInfo> fixable = ReadAction.compute(() -> collectUnknownSdks());
    if (fixable.isEmpty()) return;

    Map<MissingSdkInfo, LocalSdkFix> localFixes = findLocalFixes(indicator, fixable, lookups);
    Map<MissingSdkInfo, DownloadSdkFix> downloadFixes = findDownloadFixes(indicator,
                                                                          ContainerUtil.filter(fixable, info -> !localFixes.containsKey(info)),
                                                                          lookups);

    if (localFixes.isEmpty() && downloadFixes.isEmpty()) return;

    ApplicationManager.getApplication().invokeLater(() -> {
      configureLocalSdks(localFixes);

      UnknownSdkBalloonNotification.getInstance(myProject).notifyFixedSdks(localFixes);
      UnknownSdkEditorNotification.getInstance(myProject).showNotifications(downloadFixes);
    });
  }

  @NotNull
  private List<MissingSdkInfo> collectUnknownSdks() {
    Map<String, MissingSdkInfo> myInfos = new HashMap<>();
    List<SdkUsage> usages = SdkUsagesCollector.getInstance(myProject).collectSdkUsages();
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

  public void applyDownloadableFix(@NotNull UnknownSdk info, @NotNull DownloadSdkFix fix) {
    SdkDownloadTask task;
    String title = "Configuring SDK";
    try {
      task = ProgressManager.getInstance().run(new Task.WithResult<SdkDownloadTask, RuntimeException>(myProject, title, true) {
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

    ApplicationManager.getApplication().invokeLater(() -> {
      Disposable lifetime = Disposer.newDisposable();
      Disposer.register(myProject, lifetime);

      Sdk sdk = createSdkPrototype(info);

      SdkDownloadTracker downloadTracker = SdkDownloadTracker.getInstance();
      downloadTracker.registerSdkDownload(sdk, task);
      downloadTracker.tryRegisterDownloadingListener(sdk, lifetime, new ProgressIndicatorBase(), succeeded -> {
        if (succeeded) {
          registerNewSdkInJdkTable(info, sdk);
        }
        Disposer.dispose(lifetime);
      });

      downloadTracker.startSdkDownloadIfNeeded(sdk);
    });
  }

  public void showSdkSelectionPopup(@NotNull UnknownSdk info,
                                    @NotNull JComponent panel,
                                    @NotNull Runnable onSelectionMade) {
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
        registerNewSdkInJdkTable(info, sdk);
        wasSdkCreated.set(true);
      }
    });

    //FileEditorManager#addTopComponent wraps the panel to implement borders, unwrapping
    Container container = panel.getParent();
    if (container == null) container = panel;
    popup.showUnderneathToTheRightOf(
      container,
      () -> {
        if (wasSdkCreated.get()) {
          onSelectionMade.run();
        }
      }
    );
  }

  private static void configureLocalSdks(@NotNull Map<MissingSdkInfo, LocalSdkFix> localFixes) {
    for (Map.Entry<MissingSdkInfo, LocalSdkFix> e : localFixes.entrySet()) {
      MissingSdkInfo info = e.getKey();
      LocalSdkFix fix = e.getValue();

      Sdk sdk = createSdkPrototype(info);
      SdkModificator mod = sdk.getSdkModificator();
      mod.setHomePath(FileUtil.toSystemIndependentName(fix.getExistingSdkHome()));
      mod.setVersionString(fix.getVersionString());
      mod.commitChanges();

      try {
        info.getSdkType().setupSdkPaths(sdk);
      } catch (Exception error) {
        LOG.warn("Failed to setupPaths for " + sdk + ". " + error.getMessage(), error);
      }

      registerNewSdkInJdkTable(info, sdk);
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
  private static Sdk createSdkPrototype(@NotNull UnknownSdk info) {
    return ProjectJdkTable.getInstance().createSdk(info.getSdkName(), info.getSdkType());
  }

  private static void registerNewSdkInJdkTable(@NotNull UnknownSdk info, @NotNull Sdk sdk) {
    WriteAction.run(() -> {
      ProjectJdkTable table = ProjectJdkTable.getInstance();
      Sdk clash = table.findJdk(info.getSdkName());
      if (clash != null) {
        LOG.warn("SDK with name " + info.getSdkName() + " already exists: clash=" + clash + ", new=" + sdk);
        return;
      }

      SdkModificator mod = sdk.getSdkModificator();
      mod.setName(info.getSdkName());
      mod.commitChanges();

      table.addJdk(sdk);
    });
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
}
