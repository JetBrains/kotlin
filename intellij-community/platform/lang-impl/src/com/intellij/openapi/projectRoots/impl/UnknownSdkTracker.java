// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.projectRoots.impl;

import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.SetMultimap;
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
import com.intellij.openapi.projectRoots.ProjectJdkTable;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkModificator;
import com.intellij.openapi.projectRoots.SdkType;
import com.intellij.openapi.projectRoots.impl.SdkUsagesCollector.SdkUsage;
import com.intellij.openapi.projectRoots.impl.UnknownSdkResolver.DownloadSdkFix;
import com.intellij.openapi.projectRoots.impl.UnknownSdkResolver.LocalSdkFix;
import com.intellij.openapi.projectRoots.impl.UnknownSdkResolver.UnknownSdkLookup;
import com.intellij.openapi.roots.ui.configuration.SdkListItem;
import com.intellij.openapi.roots.ui.configuration.SdkListModelBuilder;
import com.intellij.openapi.roots.ui.configuration.SdkPopup;
import com.intellij.openapi.roots.ui.configuration.SdkPopupFactory;
import com.intellij.openapi.roots.ui.configuration.projectRoot.ProjectSdksModel;
import com.intellij.openapi.roots.ui.configuration.projectRoot.SdkDownloadTask;
import com.intellij.openapi.roots.ui.configuration.projectRoot.SdkDownloadTracker;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.util.TripleFunction;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.*;

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

  //TODO: call update on ProjectSdkTable change (otherwise ProjectSdkValidation notifications will not disappear)
  public void updateUnknownSdks() {
    if (!UnknownSdkResolver.EP_NAME.hasAnyExtensions()) return;
    List<String> missingSdks = new ArrayList<>();
    List<UnknownSdk> fixable = new ArrayList<>();

    ReadAction.nonBlocking(() -> {
      collectAndGroupSdkUsages(missingSdks, fixable);
    })
    .expireWith(myProject)
    .coalesceBy(this)
    .submit(AppExecutorUtil.getAppExecutorService())
    .onSuccess((__) -> {
      onFixableAndMissingSdksCollected( missingSdks, fixable);
    });
  }

  private void onFixableAndMissingSdksCollected(@NotNull List<String> missingSdks,
                                                @NotNull List<UnknownSdk> fixable) {


    if (!fixable.isEmpty()) {
      ProgressManager.getInstance()
        .run(new Task.Backgroundable(myProject, "Resolving SDKs", false, ALWAYS_BACKGROUND) {
               @Override
               public void run(@NotNull ProgressIndicator indicator) {
                 indicator.setText("Resolving missing SDKs...");
                 List<UnknownSdkLookup> lookups = collectSdkLookups(indicator);

                 indicator.setText("Looking for local SDKs...");
                 Map<UnknownSdk, LocalSdkFix> localFixes =
                   findFixesAndRemoveFixable(indicator, fixable, lookups, UnknownSdkLookup::proposeLocalFix);

                 indicator.setText("Looking for downloadable SDKs...");
                 Map<UnknownSdk, DownloadSdkFix> downloadFixes =
                   findFixesAndRemoveFixable(indicator, fixable, lookups, UnknownSdkLookup::proposeDownload);
                 fixable.forEach(it -> missingSdks.add(it.getSdkName()));

                 ApplicationManager.getApplication().invokeLater(() -> {
                   indicator.setText("Configuring SDKs...");
                   configureLocalSdks(localFixes);
                 });

                 showStatus(missingSdks, localFixes, downloadFixes);
               }
             }
        );
    } else {
      showStatus(missingSdks, Collections.emptyMap(), Collections.emptyMap());
    }
  }

  private void showStatus(@NotNull List<String> missingSdks,
                          @NotNull Map<UnknownSdk, LocalSdkFix> localFixes,
                          @NotNull Map<UnknownSdk, DownloadSdkFix> downloadFixes) {
    ApplicationManager.getApplication().invokeLater(() -> {
      UnknownSdkBalloonNotification
        .getInstance(myProject)
        .notifyFixedSdks(localFixes);

      UnknownSdkEditorNotification
        .getInstance(myProject)
        .showNotifications(missingSdks.isEmpty() && downloadFixes.isEmpty(),
                           missingSdks,
                           downloadFixes);

    }, myProject.getDisposed());
  }

  @NotNull
  private List<UnknownSdkLookup> collectSdkLookups(@NotNull ProgressIndicator indicator) {
    List<UnknownSdkLookup> lookups = new ArrayList<>();
    UnknownSdkResolver.EP_NAME.forEachExtensionSafe(ext -> {
      UnknownSdkLookup resolver = ext.createResolver(myProject, indicator);
      if (resolver != null) {
        lookups.add(resolver);
      }
    });
    return lookups;
  }

  /**
   * Collects all SDK usages from the project model and splits them
   * into the specified groups
   * @param totallyUnknownSdks all named SDKs that are not present and where SdkType is missing or contains different values
   * @param resolvableSdks all usages where fix extension points from {@link UnknownSdkResolver#EP_NAME} are possible to apply
   */
  private void collectAndGroupSdkUsages(@NotNull List<String> totallyUnknownSdks,
                                        @NotNull List<UnknownSdk> resolvableSdks) {
    //task is executed under non-blocking ReadAction, thus must be cleared on every run

    totallyUnknownSdks.clear();
    resolvableSdks.clear();
    SetMultimap<String, String> sdkToTypes = MultimapBuilder
      .treeKeys(String.CASE_INSENSITIVE_ORDER)
      .hashSetValues()
      .build();

    ProjectJdkTable jdkTable = ProjectJdkTable.getInstance();
    for (SdkUsage usage : SdkUsagesCollector.getInstance(myProject).collectSdkUsages()) {
      String sdkName = usage.getSdkName();

      if (sdkName == null) {
        continue;
      }

      //we do not track existing SDKs
      if (jdkTable.findJdk(sdkName) != null) continue;

      String typeName = usage.getSdkTypeName();
      sdkToTypes.put(sdkName, typeName);
    }

    for (Map.Entry<String, Collection<String>> entry : sdkToTypes.asMap().entrySet()) {
      Collection<String> sdkTypes = entry.getValue();

      SdkType sdkType = null;
      if (sdkTypes.size() == 1) {
        sdkType = SdkType.findByName(ContainerUtil.getFirstItem(sdkTypes));
      }

      String sdkName = entry.getKey();
      if (sdkType == null) {
        totallyUnknownSdks.add(sdkName);
      }
      else {
        resolvableSdks.add(new MissingSdkInfo(sdkName, sdkType));
      }
    }
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
      downloadTracker.tryRegisterDownloadingListener(sdk, lifetime, new ProgressIndicatorBase(), __ -> Disposer.dispose(lifetime));
      downloadTracker.startSdkDownloadIfNeeded(sdk);
      registerNewSdkInJdkTable(info.getSdkName(), sdk);
      updateUnknownSdks();
    });
  }

  public void showSdkSelectionPopup(@Nullable String sdkName,
                                    @Nullable SdkType sdkType,
                                    @NotNull JComponent underneathRightOfComponent) {
    ProjectSdksModel model = new ProjectSdksModel();
    SdkListModelBuilder modelBuilder = new SdkListModelBuilder(
      myProject,
      model,
      sdkType != null ? type -> Objects.equals(type, sdkType) : null,
      null,
      null);

    SdkPopupFactory popup = new SdkPopupFactory(
      myProject,
      model,
      modelBuilder
    );

    popup.createPopup(underneathRightOfComponent, new SdkPopup.SdkPopupListener() {
      private void handleNewItem(@NotNull SdkListItem item) {
        if (item instanceof SdkListItem.SdkItem) {
          Sdk sdk = ((SdkListItem.SdkItem)item).getSdk();
          registerNewSdkInJdkTable(sdkName, sdk);
          updateUnknownSdks();
        }
      }

      @Override
      public void onNewItemAdded(@NotNull SdkListItem item) {
        handleNewItem(item);
      }

      @Override
      public void onExistingItemSelected(@NotNull SdkListItem item) {
        handleNewItem(item);
      }
    }).showUnderneathToTheRightOf(underneathRightOfComponent);
  }

  private void configureLocalSdks(@NotNull Map<UnknownSdk, LocalSdkFix> localFixes) {
    if (localFixes.isEmpty()) return;

    for (Map.Entry<UnknownSdk, LocalSdkFix> e : localFixes.entrySet()) {
      UnknownSdk info = e.getKey();
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

      registerNewSdkInJdkTable(info.getSdkName(), sdk);
      LOG.info("Automatically set Sdk " + info.getSdkName() + " to " + fix.getExistingSdkHome());
    }

    updateUnknownSdks();
  }

  @NotNull
  private static <R> Map<UnknownSdk, R> findFixesAndRemoveFixable(@NotNull ProgressIndicator indicator,
                                                                  @NotNull List<UnknownSdk> infos,
                                                                  @NotNull List<UnknownSdkLookup> lookups,
                                                                  @NotNull TripleFunction<UnknownSdkLookup, UnknownSdk, ProgressIndicator, R> fun) {
    Map<UnknownSdk, R> result = new LinkedHashMap<>();
    for (Iterator<UnknownSdk> iterator = infos.iterator(); iterator.hasNext(); ) {
      UnknownSdk info = iterator.next();
      for (UnknownSdkLookup lookup : lookups) {
        R fix = fun.fun(lookup, info, indicator);
        if (fix != null) {
          result.put(info, fix);
          iterator.remove();
          break;
        }
      }
    }
    return result;
  }

  @NotNull
  private static Sdk createSdkPrototype(@NotNull UnknownSdk info) {
    return ProjectJdkTable.getInstance().createSdk(info.getSdkName(), info.getSdkType());
  }

  private static void registerNewSdkInJdkTable(@Nullable String sdkName, @NotNull Sdk sdk) {
    WriteAction.run(() -> {
      ProjectJdkTable table = ProjectJdkTable.getInstance();
      if (sdkName != null) {
        Sdk clash = table.findJdk(sdkName);
        if (clash != null) {
          LOG.warn("SDK with name " + sdkName + " already exists: clash=" + clash + ", new=" + sdk);
          return;
        }

        SdkModificator mod = sdk.getSdkModificator();
        mod.setName(sdkName);
        mod.commitChanges();
      }

      table.addJdk(sdk);
    });
  }

  private static class MissingSdkInfo implements UnknownSdk {
    @NotNull private final String mySdkName;
    @NotNull private final SdkType mySdkType;

    private MissingSdkInfo(@NotNull String sdkName, @NotNull SdkType sdkType) {
      mySdkName = sdkName;
      mySdkType = sdkType;
    }

    @NotNull
    @Override
    public String getSdkName() {
      return mySdkName;
    }

    @NotNull
    @Override
    public SdkType getSdkType() {
      return mySdkType;
    }
  }
}
