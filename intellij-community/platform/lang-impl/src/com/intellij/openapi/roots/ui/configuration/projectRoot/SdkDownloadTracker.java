// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.roots.ui.configuration.projectRoot;

import com.google.common.collect.Sets;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.PerformInBackgroundOption;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.progress.util.ProgressIndicatorBase;
import com.intellij.openapi.project.ProjectBundle;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkType;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.wm.ex.ProgressIndicatorEx;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class SdkDownloadTracker {
  private static final Logger LOG = Logger.getInstance(SdkDownloadTracker.class);

  @NotNull
  public static SdkDownloadTracker getInstance() {
    return ApplicationManager.getApplication().getService(SdkDownloadTracker.class);
  }

  private final List<PendingDownload> myPendingTasks = new ArrayList<>();

  @Nullable
  private PendingDownload findTask(@NotNull Sdk sdk) {
    for (PendingDownload task : myPendingTasks) {
      if (task.myEditableSdks.contains(sdk)) {
        return task;
      }
    }
    return null;
  }

  public void registerEditableSdk(@NotNull Sdk original,
                                  @NotNull Sdk editable) {
    PendingDownload task = findTask(original);
    if (task == null) return;

    LOG.assertTrue(findTask(editable) == null, "Download is already running for the Sdk " + editable);
    task.registerEditableSdk(editable);
  }

  public void registerSdkDownload(@NotNull Sdk originalSdk,
                                  @NotNull SdkDownloadTask item) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    LOG.assertTrue(findTask(originalSdk) == null, "Download is already running for the Sdk " + originalSdk);

    PendingDownload pd = new PendingDownload(originalSdk, item);
    myPendingTasks.add(pd);

    SdkType type = (SdkType)originalSdk.getSdkType();
    Task.Backgroundable task = new Task.Backgroundable(null,
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
   *
   * @param sdk                        the Sdk instance that to check (it could be it's #clone())
   * @param lifetime                   unsubscribe callback
   * @param indicator                  progress indicator to deliver progress
   * @param onDownloadCompleteCallback called once download is completed from ETD to update the UI
   * @return true if the given Sdk is downloading right now
   */
  public boolean tryRegisterDownloadingListener(@NotNull Sdk sdk,
                                                @NotNull Disposable lifetime,
                                                @NotNull ProgressIndicator indicator,
                                                @NotNull Runnable onDownloadCompleteCallback) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    PendingDownload pd = findTask(sdk);
    if (pd == null) return false;

    pd.tryRegisterListener(lifetime, indicator, onDownloadCompleteCallback);
    return true;
  }

  private class PendingDownload {
    final SdkDownloadTask myTask;

    final Set<Sdk> myEditableSdks = Sets.newIdentityHashSet();
    final ProgressIndicatorBase myProgressIndicator = new ProgressIndicatorBase();
    final Set<Runnable> myCompleteListeners = Sets.newIdentityHashSet();
    final Set<Disposable> myDisposables = Sets.newIdentityHashSet();

    private PendingDownload(@NotNull Sdk sdk,
                            @NotNull SdkDownloadTask task) {
      myEditableSdks.add(sdk);
      myTask = task;
    }

    public void registerEditableSdk(@NotNull Sdk editable) {
      // there are many Sdk clones that are created
      // along with the Project Structure model.
      // Our goal is to keep track of all such objects to make
      // sure we update all and refresh the UI once download is completed
      //
      // there is a chance we have here several unneeded objects,
      // e.g. from the Project Structure dialog is shown several
      // times. It's cheaper to ignore then to track
      myEditableSdks.add(editable);
    }

    public void doDownload(@NotNull ProgressIndicator indicator) {
      try {
        //we need a progress indicator from the outside, to avoid race condition (progress may start with a delay, but UI would need a PI)
        myProgressIndicator.addStateDelegate((ProgressIndicatorEx)indicator);
        //TODO[jo]: handle exceptions
        myTask.doDownload(myProgressIndicator);
      }
      finally {
        myProgressIndicator.removeStateDelegate((ProgressIndicatorEx)indicator);
        ApplicationManager.getApplication().invokeLater(this::onSdkDownloadCompleted);
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

    public void onSdkDownloadCompleted() {
      ApplicationManager.getApplication().assertIsDispatchThread();

      WriteAction.run(() -> {
        for (Sdk sdk : myEditableSdks) {
          ((SdkType)sdk.getSdkType()).setupSdkPaths(sdk);
        }

        myPendingTasks.remove(this);

        //free the WriteAction, thus non-blocking
        ApplicationManager.getApplication().invokeLater(() -> {
          myCompleteListeners.forEach(it -> it.run());
          for (Disposable disposable : myDisposables) {
            Disposer.dispose(disposable);
          }
        });
      });
    }
  }
}
