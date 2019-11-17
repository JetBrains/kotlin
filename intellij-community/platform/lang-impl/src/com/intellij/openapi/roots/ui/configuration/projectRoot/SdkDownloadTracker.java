// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.roots.ui.configuration.projectRoot;

import com.google.common.collect.Sets;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.progress.*;
import com.intellij.openapi.progress.util.ProgressIndicatorBase;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectBundle;
import com.intellij.openapi.projectRoots.ProjectJdkTable;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkType;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.wm.ex.ProgressIndicatorEx;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class SdkDownloadTracker {
  @NotNull
  public static SdkDownloadTracker getInstance() {
    return ApplicationManager.getApplication().getService(SdkDownloadTracker.class);
  }

  private static final Key<PendingDownload> PENDING_DOWNLOAD_KEY = Key.create(PendingDownload.class.getName());

  public void registerSdkDownload(@Nullable Project project,
                                  @NotNull Sdk originalSdk,
                                  @NotNull ProjectSdksModel model,
                                  @NotNull SdkDownloadTask item) {
    ApplicationManager.getApplication().assertIsDispatchThread();

    PendingDownload pd = new PendingDownload(model, originalSdk, item);
    originalSdk.putUserData(PENDING_DOWNLOAD_KEY, pd);

    SdkType type = (SdkType)originalSdk.getSdkType();
    Task.Backgroundable task = new Task.Backgroundable(project,
                                                       ProjectBundle.message("sdk.configure.downloading", type.getPresentableName()),
                                                       true,
                                                       PerformInBackgroundOption.ALWAYS_BACKGROUND) {
      @Override
      public void run(@NotNull ProgressIndicator indicator) {
        pd.doDownload(indicator);
      }
    };

    ProgressManager.getInstance().run(task);
  }

  /**
   * Checks if there is an activity for a given Sdk and subscribes a listeners if there is an activity
   * @param sdk the Sdk instance that to check (it could be it's #clone())
   * @param lifetime unsubscribe callback
   * @param indicator progress indicator to deliver progress
   * @param onDownloadCompleteCallback called once download is completed from ETD to update the UI
   * @return true if the given Sdk is downloading right now
   */
  public boolean tryRegisterDownloadingListener(@NotNull Sdk sdk,
                                                @NotNull Disposable lifetime,
                                                @NotNull ProgressIndicator indicator,
                                                @NotNull Runnable onDownloadCompleteCallback) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    PendingDownload pd = sdk.getUserData(PENDING_DOWNLOAD_KEY);
    if (pd == null) return false;

    if (pd.myIsCompleted.get()) {
      sdk.putUserData(PENDING_DOWNLOAD_KEY, null);
      //TODO[jo]: sanity re-setup could be done here, as long as it's completed, but not changed
      return false;
    }

    pd.tryRegisterListener(lifetime, indicator, onDownloadCompleteCallback);
    return true;
  }

  private static class PendingDownload {
    final ProjectSdksModel myModel;
    final Sdk mySdk;
    final SdkDownloadTask myTask;
    final ProgressIndicatorBase myProgressIndicator = new ProgressIndicatorBase();
    final AtomicBoolean myIsCompleted = new AtomicBoolean(false);
    final Set<Runnable> myCompleteListeners = Sets.newIdentityHashSet();
    final Set<Disposable> myDisposables = Sets.newIdentityHashSet();

    private PendingDownload(@NotNull ProjectSdksModel model,
                            @NotNull Sdk sdk,
                            @NotNull SdkDownloadTask task) {
      myModel = model;
      mySdk = sdk;
      myTask = task;
    }

    public void doDownload(@NotNull ProgressIndicator indicator) {
      try {
        //we need a progress indicator from the outside, to avoid race condition (progress may start with a delay, but UI would need a PI)
        myProgressIndicator.addStateDelegate((ProgressIndicatorEx)indicator);
        //TODO[jo]: handle exceptions
        myTask.doDownload(myProgressIndicator);
      } finally {
        myProgressIndicator.removeStateDelegate((ProgressIndicatorEx)indicator);
        ApplicationManager.getApplication().invokeLater(() -> dispose());
      }
    }

    public void tryRegisterListener(@NotNull Disposable lifetime,
                                    @NotNull ProgressIndicator uiIndicator,
                                    @NotNull Runnable completedCallback) {
      ApplicationManager.getApplication().assertIsDispatchThread();

      //there is no need to add yet another copy of the same component
      if (!myCompleteListeners.add(completedCallback)) return;

      //make the UI indicator receive events, when the background task run
      myProgressIndicator.addStateDelegate((ProgressIndicatorEx)uiIndicator);

      Disposable unsubscribe = new Disposable() {
        @Override
        public void dispose() {
          myProgressIndicator.removeStateDelegate((ProgressIndicatorEx)uiIndicator);
          myCompleteListeners.remove(completedCallback);
          myDisposables.remove(this);
        }
      };
      Disposer.register(lifetime, unsubscribe);
      myDisposables.add(unsubscribe);
    }

    public void dispose() {
      ApplicationManager.getApplication().assertIsDispatchThread();
      myIsCompleted.set(true);

      // there are several possibilities where our initial Sdk instance
      // could reach. We assume UserDataHolder is preserved between Sdk instance on #clone()
      //
      // 1. it could only be in the ProjectSdksModel (myModel)
      //   => so the mySdk is a key,
      //   => there is yet another editable clone
      //   it's OK to patch both or the editable one
      //   the first one would we registered in the ProjectJdkTable
      //
      // 2. the ProjectSdksModel (myModel) was reset,
      //   => our mySdk is registered in the ProjectJdkTable (or is removed)
      //      => we need to patch the registered one (mySdk)
      //   => we still need to patch the cloned editable in the ProjectSdksModel (myModel)
      //
      // to summarize: we patch both Sdks, and check the ProjectJdkTable, just in case
      WriteAction.run(() -> {
        Sdk modifiableSdk = myModel.getProjectSdks().get(mySdk);
        if (modifiableSdk != null) {
          setupOurSdkAndCleanTheState(modifiableSdk);
        }

        setupOurSdkAndCleanTheState(mySdk);

        for (Sdk jdk : ProjectJdkTable.getInstance().getAllJdks()) {
          setupOurSdkAndCleanTheState(jdk);
        }

        //free the WriteAction, thus non-blocking
        ApplicationManager.getApplication().invokeLater(() -> {
          myCompleteListeners.forEach(it -> it.run());
          for (Disposable disposable : myDisposables) {
            Disposer.dispose(disposable);
          }
        });
      });
    }

    private void setupOurSdkAndCleanTheState(@NotNull Sdk sdk) {
      if (sdk.getUserData(PENDING_DOWNLOAD_KEY) != this) return;
      ((SdkType)sdk.getSdkType()).setupSdkPaths(sdk);
      sdk.putUserData(PENDING_DOWNLOAD_KEY, null);
    }
  }
}
