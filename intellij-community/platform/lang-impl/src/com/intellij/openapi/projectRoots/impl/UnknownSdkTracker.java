// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.projectRoots.impl;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectBundle;
import com.intellij.openapi.projectRoots.*;
import com.intellij.openapi.roots.ui.configuration.*;
import com.intellij.openapi.roots.ui.configuration.UnknownSdkResolver.UnknownSdkLookup;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.ui.EditorNotificationPanel;
import com.intellij.util.Consumer;
import com.intellij.util.TripleFunction;
import com.intellij.util.ui.update.MergingUpdateQueue;
import com.intellij.util.ui.update.Update;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

  private UnknownSdkSnapshot myPreviousRequestCache = null;

  public UnknownSdkTracker(@NotNull Project project) {
    myProject = project;
    myUpdateQueue = new MergingUpdateQueue(getClass().getSimpleName(),
                                           700,
                                           true,
                                           null,
                                           myProject,
                                           null,
                                           false)
      .usePassThroughInUnitTestMode();
  }

  @NotNull
  private Update newUpdateTask() {
    return new Update("update") {
      @Override
      public void run() {
        if (!Registry.is("unknown.sdk") || !UnknownSdkResolver.EP_NAME.hasAnyExtensions()) {
          showStatus(Collections.emptyList(), Collections.emptyMap(), Collections.emptyMap(), Collections.emptyList());
          return;
        }

        new UnknownSdkCollector(myProject)
          .collectSdksPromise(snapshot -> {

            //there is nothing to do if we see the same snapshot, IDEA-236153
            if (snapshot.equals(myPreviousRequestCache)) return;
            myPreviousRequestCache = snapshot;

            //we cannot use snapshot#missingSdks here, because it affects other IDEs/languages where our logic is not good enough
            onFixableAndMissingSdksCollected(filterOnlyAllowedEntries(snapshot.getResolvableSdks()), filterOnlyAllowedSdkEntries(snapshot.getKnownSdks()));
          });
      }
    };
  }

  public void updateUnknownSdksNow() {
    myUpdateQueue.run(newUpdateTask());
  }

  public void updateUnknownSdks() {
    myUpdateQueue.queue(newUpdateTask());
  }

  private static boolean allowFixesFor(@NotNull SdkTypeId type) {
    return UnknownSdkResolver.EP_NAME.findFirstSafe(it -> it.supportsResolution(type)) != null;
  }

  @NotNull
  private static <E extends UnknownSdk> List<E> filterOnlyAllowedEntries(@NotNull List<? extends E> input) {
    List<E> copy = new ArrayList<>();
    for (E item : input) {
      SdkType type = item.getSdkType();

      if (allowFixesFor(type)) {
        copy.add(item);
      }
    }

    return copy;
  }

  @NotNull
  private static List<Sdk> filterOnlyAllowedSdkEntries(@NotNull List<Sdk> input) {
    List<Sdk> copy = new ArrayList<>();
    for (Sdk item : input) {
      SdkTypeId type = item.getSdkType();

      if (allowFixesFor(type)) {
        copy.add(item);
      }
    }

    return copy;
  }

  private void onFixableAndMissingSdksCollected(@NotNull List<UnknownSdk> fixable, @NotNull List<Sdk> usedSdks) {
    if (fixable.isEmpty() && usedSdks.isEmpty()) {
      showStatus(Collections.emptyList(), Collections.emptyMap(), Collections.emptyMap(), Collections.emptyList());
      return;
    }

    ProgressManager.getInstance()
      .run(new Task.Backgroundable(myProject, ProjectBundle.message("progress.title.resolving.sdks"), false, ALWAYS_BACKGROUND) {
             @Override
             public void run(@NotNull ProgressIndicator indicator) {
               List<UnknownInvalidSdk> invalidSdks = new ArrayList<>();
               Map<UnknownSdk, UnknownSdkLocalSdkFix> localFixes = new HashMap<>();
               Map<UnknownSdk, UnknownSdkDownloadableSdkFix> downloadFixes = new HashMap<>();

               if (!usedSdks.isEmpty()) {
                 indicator.pushState();
                 indicator.setText(ProjectBundle.message("progress.text.resolving.existing.sdks"));
                 invalidSdks = UnknownInvalidSdk.resolveInvalidSdks(usedSdks);
                 fixable.addAll(invalidSdks);
                 indicator.popState();
               }

               if (!fixable.isEmpty()) {
                 indicator.pushState();
                 indicator.setText(ProjectBundle.message("progress.text.resolving.missing.sdks"));
                 List<UnknownSdkLookup> lookups = collectSdkLookups(indicator);

                 if (!lookups.isEmpty()) {
                   indicator.setText(ProjectBundle.message("progress.text.looking.for.local.sdks"));
                   localFixes = findFixesAndRemoveFixable(indicator, fixable, lookups, UnknownSdkLookup::proposeLocalFix);

                   if (!fixable.isEmpty()) {
                     indicator.setText(ProjectBundle.message("progress.text.looking.for.downloadable.sdks"));
                     downloadFixes = findFixesAndRemoveFixable(indicator, fixable, lookups, UnknownSdkLookup::proposeDownload);
                   }
                 }

                 indicator.popState();
               }

               UnknownInvalidSdk.removeAndUpdate(invalidSdks, fixable, localFixes, downloadFixes);

               if (!localFixes.isEmpty()) {
                 indicator.pushState();
                 indicator.setText(ProjectBundle.message("progress.text.configuring.sdks"));
                 configureLocalSdks(localFixes);
                 indicator.popState();
               }

               showStatus(fixable, localFixes, downloadFixes, invalidSdks);
             }
           }
      );
  }

  private void showStatus(@NotNull List<UnknownSdk> unknownSdksWithoutFix,
                          @NotNull Map<UnknownSdk, UnknownSdkLocalSdkFix> localFixes,
                          @NotNull Map<UnknownSdk, UnknownSdkDownloadableSdkFix> downloadFixes,
                          @NotNull List<UnknownInvalidSdk> invalidSdks) {
    UnknownSdkBalloonNotification
      .getInstance(myProject)
      .notifyFixedSdks(localFixes);

    UnknownSdkEditorNotification
      .getInstance(myProject)
      .showNotifications(unknownSdksWithoutFix, downloadFixes, invalidSdks);

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
        updateUnknownSdksNow();
      }
    });
  }

  @ApiStatus.Internal
  public static void downloadFix(@Nullable Project project,
                                 @NotNull UnknownSdk info,
                                 @NotNull UnknownSdkDownloadableSdkFix fix,
                                 @NotNull Consumer<? super Sdk> onSdkNameReady,
                                 @NotNull Consumer<? super Sdk> onCompleted) {
    UnknownSdkDownloader.downloadFix(project, info, fix,
                task -> {
                  String actualSdkName = info.getSdkName();
                  if (actualSdkName == null) {
                    actualSdkName = task.getSuggestedSdkName();
                  }
                  return ProjectJdkTable.getInstance().createSdk(actualSdkName, info.getSdkType());
                },
                onSdkNameReady,
                sdk -> {
                  if (sdk != null) {
                    fix.configureSdk(sdk);
                    registerNewSdkInJdkTable(sdk.getName(), sdk);
                  }
                  onCompleted.consume(sdk);
                });
  }

  @NotNull
  public EditorNotificationPanel.ActionHandler createSdkSelectionPopup(@Nullable String sdkName,
                                                                       @Nullable SdkType sdkType) {
    return SdkPopupFactory
      .newBuilder()
      .withProject(myProject)
      .withSdkTypeFilter(type -> sdkType == null || Objects.equals(type, sdkType))
      .onSdkSelected(sdk -> {
        registerNewSdkInJdkTable(sdkName, sdk);
        updateUnknownSdks();
      })
      .buildEditorNotificationPanelHandler();
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
        fix.configureSdk(sdk);
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
