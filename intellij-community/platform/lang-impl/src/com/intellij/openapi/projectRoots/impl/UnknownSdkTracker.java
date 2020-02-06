// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.projectRoots.impl;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
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
import com.intellij.openapi.roots.ui.configuration.*;
import com.intellij.openapi.roots.ui.configuration.UnknownSdkDownloadableSdkFix;
import com.intellij.openapi.roots.ui.configuration.UnknownSdkResolver.UnknownSdkLookup;
import com.intellij.openapi.roots.ui.configuration.projectRoot.SdkDownloadTask;
import com.intellij.openapi.roots.ui.configuration.projectRoot.SdkDownloadTracker;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.util.Consumer;
import com.intellij.util.TripleFunction;
import com.intellij.util.ui.update.MergingUpdateQueue;
import com.intellij.util.ui.update.Update;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.*;

import static com.intellij.openapi.progress.PerformInBackgroundOption.ALWAYS_BACKGROUND;

public class UnknownSdkTracker {
  private static final Logger LOG = Logger.getInstance(UnknownSdkTracker.class);

  @NotNull
  public static UnknownSdkTracker getInstance(@NotNull Project project) {
    return project.getService(UnknownSdkTracker.class);
  }

  @NotNull private final Project myProject;
  @NotNull private final MergingUpdateQueue myUpdateQueue;

  public UnknownSdkTracker(@NotNull Project project) {
    myProject = project;
    myUpdateQueue = new MergingUpdateQueue(getClass().getSimpleName(),
                                           200,
                                           true,
                                           null,
                                           myProject,
                                           null,
                                           false)
      .usePassThroughInUnitTestMode();
  }

  public void updateUnknownSdks() {
    myUpdateQueue.run(new Update("update") {
      @Override
      public void run() {
        if (!Registry.is("unknown.sdk") || !UnknownSdkResolver.EP_NAME.hasAnyExtensions()) {
          showStatus(Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap());
          return;
        }

        new UnknownSdkCollector(myProject)
          .collectSdksPromise(snapshot -> {
            onFixableAndMissingSdksCollected(snapshot);
          });
      }
    });
  }

  private void onFixableAndMissingSdksCollected(@NotNull UnknownSdkSnapshot snapshot) {
    final Map<String, SdkType> missingSdks = new LinkedHashMap<>();
    final List<UnknownSdk> fixable = new ArrayList<>(snapshot.getResolvableSdks());
    for (String sdk : snapshot.getTotallyUnknownSdks()) {
      missingSdks.put(sdk, null);
    }

    if (snapshot.getResolvableSdks().isEmpty()) {
      showStatus(missingSdks, Collections.emptyMap(), Collections.emptyMap());
      return;
    }

    ProgressManager.getInstance()
      .run(new Task.Backgroundable(myProject, "Resolving SDKs", false, ALWAYS_BACKGROUND) {
             @Override
             public void run(@NotNull ProgressIndicator indicator) {
               indicator.setText("Resolving missing SDKs...");
               List<UnknownSdkLookup> lookups = collectSdkLookups(indicator);

               indicator.setText("Looking for local SDKs...");
               Map<UnknownSdk, UnknownSdkLocalSdkFix> localFixes = findFixesAndRemoveFixable(indicator, fixable, lookups, UnknownSdkLookup::proposeLocalFix);

               indicator.setText("Looking for downloadable SDKs...");
               Map<UnknownSdk, UnknownSdkDownloadableSdkFix> downloadFixes = findFixesAndRemoveFixable(indicator, fixable, lookups, UnknownSdkLookup::proposeDownload);
               fixable.forEach(it -> missingSdks.put(it.getSdkName(), it.getSdkType()));

               if (!localFixes.isEmpty()) {
                 indicator.setText("Configuring SDKs...");
                 configureLocalSdks(localFixes);
               }

               showStatus(missingSdks, localFixes, downloadFixes);
             }
           }
      );
  }

  private void showStatus(@NotNull Map<String, SdkType> missingSdks,
                          @NotNull Map<UnknownSdk, UnknownSdkLocalSdkFix> localFixes,
                          @NotNull Map<UnknownSdk, UnknownSdkDownloadableSdkFix> downloadFixes) {
    UnknownSdkBalloonNotification
      .getInstance(myProject)
      .notifyFixedSdks(localFixes);

    boolean allowProjectSdkExtensions = missingSdks.isEmpty() && downloadFixes.isEmpty();
    if (!Registry.is("unknown.sdk.show.editor.actions")) {
      missingSdks.clear();
      downloadFixes.clear();
      allowProjectSdkExtensions = true;
    }

    UnknownSdkEditorNotification
      .getInstance(myProject)
      .showNotifications(allowProjectSdkExtensions,
                         missingSdks,
                         downloadFixes);

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

  public void applyDownloadableFix(@NotNull UnknownSdk info, @NotNull UnknownSdkDownloadableSdkFix fix) {
    downloadFix(myProject, info, fix, sdk -> {}, sdk -> {
      if (sdk != null) {
        updateUnknownSdks();
      }
    });
  }

  @ApiStatus.Internal
  public static void downloadFix(@Nullable Project project,
                                 @NotNull UnknownSdk info,
                                 @NotNull UnknownSdkDownloadableSdkFix fix,
                                 @NotNull Consumer<? super Sdk> onSdkNameReady,
                                 @NotNull Consumer<? super Sdk> onCompleted) {
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
      onCompleted.consume(null);
      throw e;
    } catch (Exception error) {
      LOG.warn("Failed to download " + info.getSdkType().getPresentableName() + " " + fix.getDownloadDescription() + " for " + info + ". " + error.getMessage(), error);
      ApplicationManager.getApplication().invokeLater(() -> {
        Messages.showErrorDialog("Failed to download " + fix.getDownloadDescription() + ". " + error.getMessage(), title);
      });
      onCompleted.consume(null);
      return;
    }

    ApplicationManager.getApplication().invokeLater(() -> {
      try {
        Disposable lifetime = Disposer.newDisposable();

        String actualSdkName = info.getSdkName();
        if (actualSdkName == null) {
          actualSdkName = task.getSuggestedSdkName();
        }

        Sdk sdk = ProjectJdkTable.getInstance().createSdk(actualSdkName, info.getSdkType());

        SdkDownloadTracker downloadTracker = SdkDownloadTracker.getInstance();
        downloadTracker.registerSdkDownload(sdk, task);
        downloadTracker.tryRegisterDownloadingListener(sdk, lifetime, new ProgressIndicatorBase(), success -> {
          Disposer.dispose(lifetime);
          onCompleted.consume(success ? sdk : null);
        });

        registerNewSdkInJdkTable(actualSdkName, sdk);
        onSdkNameReady.consume(sdk);

        downloadTracker.startSdkDownloadIfNeeded(sdk);

      } catch (Exception error) {
        LOG.warn("Failed to download " + info.getSdkType().getPresentableName() + " " + fix.getDownloadDescription() + " for " + info + ". " + error.getMessage(), error);
        ApplicationManager.getApplication().invokeLater(() -> {
          Messages.showErrorDialog("Failed to download " + fix.getDownloadDescription() + ". " + error.getMessage(), title);
        });
        onCompleted.consume(null);
      }
    });
  }

  public void showSdkSelectionPopup(@Nullable String sdkName,
                                    @Nullable SdkType sdkType,
                                    @NotNull JComponent underneathRightOfComponent) {
    SdkPopupFactory
      .newBuilder()
      .withProject(myProject)
      .withSdkTypeFilter(type -> sdkType == null || Objects.equals(type, sdkType))
      .onSdkSelected(sdk -> {
        registerNewSdkInJdkTable(sdkName, sdk);
        updateUnknownSdks();
      })
      .buildPopup()
      .showUnderneathToTheRightOf(underneathRightOfComponent);
  }

  private void configureLocalSdks(@NotNull Map<UnknownSdk, UnknownSdkLocalSdkFix> localFixes) {
    if (localFixes.isEmpty()) return;

    for (Map.Entry<UnknownSdk, UnknownSdkLocalSdkFix> e : localFixes.entrySet()) {
      UnknownSdk info = e.getKey();
      UnknownSdkLocalSdkFix fix = e.getValue();

      configureLocalSdk(info, fix, sdk -> {});
    }

    updateUnknownSdks();
  }

  @ApiStatus.Internal
  public static void configureLocalSdk(@NotNull UnknownSdk info,
                                       @NotNull UnknownSdkLocalSdkFix fix,
                                       @NotNull Consumer<? super Sdk> onCompleted) {
    ApplicationManager.getApplication().invokeLater(() -> {
      try {
        String actualSdkName = info.getSdkName();
        if (actualSdkName == null) {
          actualSdkName = fix.getSuggestedSdkName();
        }

        Sdk sdk = ProjectJdkTable.getInstance().createSdk(actualSdkName, info.getSdkType());
        SdkModificator mod = sdk.getSdkModificator();
        mod.setHomePath(FileUtil.toSystemIndependentName(fix.getExistingSdkHome()));
        mod.setVersionString(fix.getVersionString());
        mod.commitChanges();

        try {
          info.getSdkType().setupSdkPaths(sdk);
        }
        catch (Exception error) {
          LOG.warn("Failed to setupPaths for " + sdk + ". " + error.getMessage(), error);
        }

        registerNewSdkInJdkTable(actualSdkName, sdk);
        LOG.info("Automatically set Sdk " + info + " to " + fix.getExistingSdkHome());
        onCompleted.consume(sdk);
      } catch (Exception error) {
        LOG.warn("Failed to configure " + info.getSdkType().getPresentableName() + " " + " for " + info + " for path " + fix + ". " + error.getMessage(), error);
        onCompleted.consume(null);
      }
    });
  }

  @NotNull
  private static <R> Map<UnknownSdk, R> findFixesAndRemoveFixable(@NotNull ProgressIndicator indicator,
                                                                  @NotNull List<UnknownSdk> infos,
                                                                  @NotNull List<UnknownSdkLookup> lookups,
                                                                  @NotNull TripleFunction<UnknownSdkLookup, UnknownSdk, ProgressIndicator, R> fun) {
    indicator.pushState();

    Map<UnknownSdk, R> result = new LinkedHashMap<>();
    for (Iterator<UnknownSdk> iterator = infos.iterator(); iterator.hasNext(); ) {
      UnknownSdk info = iterator.next();
      for (UnknownSdkLookup lookup : lookups) {

        indicator.pushState();
        R fix = fun.fun(lookup, info, indicator);
        indicator.popState();

        if (fix != null) {
          result.put(info, fix);
          iterator.remove();
          break;
        }
      }
    }

    indicator.popState();
    return result;
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
}
