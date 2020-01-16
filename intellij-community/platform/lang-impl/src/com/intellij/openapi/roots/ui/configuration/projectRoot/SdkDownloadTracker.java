// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.roots.ui.configuration.projectRoot;

import com.google.common.collect.Sets;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.TransactionGuard;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.PerformInBackgroundOption;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.progress.util.ProgressIndicatorBase;
import com.intellij.openapi.project.ProjectBundle;
import com.intellij.openapi.projectRoots.ProjectJdkTable;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkModificator;
import com.intellij.openapi.projectRoots.SdkType;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.wm.ex.ProgressIndicatorEx;
import com.intellij.util.Consumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

public class SdkDownloadTracker implements Disposable {
  private static final Logger LOG = Logger.getInstance(SdkDownloadTracker.class);

  @NotNull
  public static SdkDownloadTracker getInstance() {
    return ApplicationManager.getApplication().getService(SdkDownloadTracker.class);
  }

  private final List<PendingDownload> myPendingTasks = new CopyOnWriteArrayList<>();

  public SdkDownloadTracker() {
    ApplicationManager.getApplication().getMessageBus()
      .connect(this)
      .subscribe(ProjectJdkTable.JDK_TABLE_TOPIC,
                 new ProjectJdkTable.Adapter() {
                   @Override
                   public void jdkRemoved(@NotNull Sdk jdk) {
                     onSdkRemoved(jdk);
                   }
                 });
  }

  @Override
  public void dispose() {
  }

  public void onSdkRemoved(@NotNull Sdk sdk) {
    //can be executed in any thread too, JMM safe

    PendingDownload task = findTask(sdk);
    if (task == null) return;
    task.cancel();
  }

  private void removeTask(@NotNull PendingDownload task) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    myPendingTasks.remove(task);
  }

  @Nullable
  private PendingDownload findTask(@NotNull Sdk sdk) {
    for (PendingDownload task : myPendingTasks) {
      if (task.containsSdk(sdk)) {
        return task;
      }
    }
    return null;
  }

  public void registerEditableSdk(@NotNull Sdk original,
                                  @NotNull Sdk editable) {
    // This may happen in the background thread on a project open (JMM safe)
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
    pd.configureSdk(originalSdk);
    myPendingTasks.add(pd);
  }

  public void startSdkDownloadIfNeeded(@NotNull Sdk sdkFromTable) {
    ApplicationManager.getApplication().assertIsDispatchThread();

    PendingDownload task = findTask(sdkFromTable);
    if (task == null) return;

    task.startDownloadIfNeeded(sdkFromTable);
  }

  /**
   * Checks if there is an activity for a given Sdk
   * @return true is there is an activity
   */
  public boolean isDownloading(@NotNull Sdk sdk) {
    return findTask(sdk) != null;
  }

  /**
   * Checks if there is an activity for a given Sdk and subscribes a listeners if there is an activity
   *
   * @param sdk                        the Sdk instance that to check (it could be it's #clone())
   * @param lifetime                   unsubscribe callback
   * @param indicator                  progress indicator to deliver progress
   * @param onDownloadCompleteCallback called once (with {@code true} to indicate success and {@code false} for a failure)
   *                                   download is completed from ETD to update the UI,
   * @return true if the given Sdk is downloading right now
   */
  public boolean tryRegisterDownloadingListener(@NotNull Sdk sdk,
                                                @NotNull Disposable lifetime,
                                                @NotNull ProgressIndicator indicator,
                                                @NotNull Consumer<Boolean> onDownloadCompleteCallback) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    PendingDownload pd = findTask(sdk);
    if (pd == null) return false;

    pd.registerListener(lifetime, indicator, onDownloadCompleteCallback);
    return true;
  }

  // we need to track the "best" modality state to trigger SDK update on completion,
  // while the current Project Structure dialog is shown. The ModalityState from our
  // background task (ProgressManager.run) does not suite if the Project Structure dialog
  // is re-open once again.
  //
  // We grab the modalityState from the {@link #tryRegisterDownloadingListener} call and
  // see if that {@link ModalityState#dominates} the current modality state. In fact,
  // it does call the method from the dialog setup, with NON_MODAL modality, which
  // we would like to ignore.
  private static class PendingDownloadModalityTracker {
    @NotNull
    static ModalityState modality() {
      ModalityState state = ApplicationManager.getApplication().getCurrentModalityState();
      TransactionGuard.getInstance().assertWriteSafeContext(state);
      return state;
    }

    ModalityState myModalityState = modality();

    synchronized void updateModality() {
      ModalityState newModality = modality();
      if (newModality != myModalityState && newModality.dominates(myModalityState)) {
        myModalityState = newModality;
      }
    }

    synchronized void invokeLater(@NotNull Runnable r) {
      ApplicationManager.getApplication().invokeLater(r, myModalityState);
    }
  }

  // synchronized newIdentityHashSet
  // (Collections.synchronizedSet does not help the iterator)
  private static class SdkSet {
    final Set<Sdk> myEditableSdks = Sets.newIdentityHashSet();

    synchronized void add(@NotNull Sdk sdk) {
      myEditableSdks.add(sdk);
    }

    synchronized boolean contains(@NotNull Sdk sdk) {
      return myEditableSdks.contains(sdk);
    }

    @NotNull
    synchronized List<Sdk> copy() {
      return new ArrayList<>(myEditableSdks);
    }
  }

  private static class PendingDownload {
    final SdkDownloadTask myTask;
    final SdkSet myEditableSdks = new SdkSet();
    final ProgressIndicatorBase myProgressIndicator = new ProgressIndicatorBase();
    final Set<Consumer<Boolean>> myCompleteListeners = Sets.newIdentityHashSet();
    final Set<Disposable> myDisposables = Sets.newIdentityHashSet();
    final PendingDownloadModalityTracker myModalityTracker = new PendingDownloadModalityTracker();

    boolean myIsDownloading = false;

    PendingDownload(@NotNull Sdk sdk, @NotNull SdkDownloadTask task) {
      myEditableSdks.add(sdk);
      myTask = task;
    }

    boolean containsSdk(@NotNull Sdk sdk) {
      // called from any thread
      return myEditableSdks.contains(sdk);
    }

    void registerEditableSdk(@NotNull Sdk editable) {
      // called from any thread
      //
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

    void startDownloadIfNeeded(@NotNull Sdk sdkFromTable) {
      ApplicationManager.getApplication().assertIsDispatchThread();

      if (myIsDownloading || myProgressIndicator.isCanceled()) return;
      myIsDownloading = true;

      myModalityTracker.updateModality();
      SdkType type = (SdkType)sdkFromTable.getSdkType();
      String message = ProjectBundle.message("sdk.configure.downloading", type.getPresentableName());

      Task.Backgroundable task = new Task.Backgroundable(null,
                                                         message,
                                                         true,
                                                         PerformInBackgroundOption.ALWAYS_BACKGROUND) {
        @Override
        public void run(@NotNull ProgressIndicator indicator) {
          try {
            // we need a progress indicator from the outside, to avoid race condition
            // (progress may start with a delay, but UI would need a PI)
            ProgressIndicatorBase middleMan = new ProgressIndicatorBase() {
              @Override
              protected void delegateProgressChange(@NotNull IndicatorAction action) {
                action.execute((ProgressIndicatorEx)indicator);
              }
            };
            myProgressIndicator.addStateDelegate(middleMan);
            myProgressIndicator.checkCanceled();
            try {
              myTask.doDownload(myProgressIndicator);
            }
            finally {
              myProgressIndicator.removeStateDelegate(middleMan);
            }

            onSdkDownloadCompleted(false);
          }
          catch (Exception e) {
            if (!myProgressIndicator.isCanceled()) {
              LOG.warn("SDK Download failed. " + e.getMessage(), e);
              myModalityTracker.invokeLater(() -> {
                Messages.showErrorDialog(e.getMessage(), getTitle());
              });
            }
            onSdkDownloadCompleted(true);
          }
        }
      };

      ProgressManager.getInstance().run(task);
    }

    void registerListener(@NotNull Disposable lifetime,
                          @NotNull ProgressIndicator uiIndicator,
                          @NotNull Consumer<Boolean> completedCallback) {
      ApplicationManager.getApplication().assertIsDispatchThread();
      myModalityTracker.updateModality();

      //there is no need to add yet another copy of the same component
      if (!myCompleteListeners.add(completedCallback)) return;

      //make the UI indicator receive events, when the background task run
      myProgressIndicator.addStateDelegate((ProgressIndicatorEx)uiIndicator);

      Disposable unsubscribe = new Disposable() {
        @Override
        public void dispose() {
          ApplicationManager.getApplication().assertIsDispatchThread();

          myProgressIndicator.removeStateDelegate((ProgressIndicatorEx)uiIndicator);
          myCompleteListeners.remove(completedCallback);
          myDisposables.remove(this);
        }
      };
      Disposer.register(lifetime, unsubscribe);
      myDisposables.add(unsubscribe);
    }

    void onSdkDownloadCompleted(boolean failed) {
      if (!failed) {
        // we handle ModalityState explicitly to handle the case,
        // when the next ProjectSettings dialog is shown, and we still want to
        // notify all current viewers to reflect our SDK changes, thus we need
        // it's newer ModalityState to invoke. Using ModalityState.any is not
        // an option as we do update Sdk instances in the call
        myModalityTracker.invokeLater(() -> WriteAction.run(() -> {
          for (Sdk sdk : myEditableSdks.copy()) {
            try {
              SdkType sdkType = (SdkType)sdk.getSdkType();
              configureSdk(sdk);

              try {
                String actualVersion = sdkType.getVersionString(sdk);
                SdkModificator modificator = sdk.getSdkModificator();
                modificator.setVersionString(actualVersion);
                modificator.commitChanges();
              } catch (Exception e) {
                LOG.warn("Failed to configure a downloaded sdk. " + e.getMessage(), e);
              }

              sdkType.setupSdkPaths(sdk);
            }
            catch (Exception e) {
              LOG.warn("Failed to setup Sdk " + sdk + ". " + e.getMessage(), e);
            }
          }
        }));
      }

      // dispose our own state
      myModalityTracker.invokeLater(() -> disposeNow(!failed));
    }

    void disposeNow(boolean succeeded) {
      ApplicationManager.getApplication().assertIsDispatchThread();
      getInstance().removeTask(this);
      //collections may change from the callbacks
      new ArrayList<>(myCompleteListeners).forEach(it -> it.consume(succeeded));
      new ArrayList<>(myDisposables).forEach(it -> Disposer.dispose(it));
    }

    void cancel() {
      myProgressIndicator.cancel();
      myModalityTracker.invokeLater(() -> disposeNow(false));
    }

    void configureSdk(@NotNull Sdk sdk) {
      SdkModificator mod = sdk.getSdkModificator();
      mod.setVersionString(myTask.getPlannedVersion());
      mod.setHomePath(FileUtil.toSystemIndependentName(myTask.getPlannedHomeDir()));
      mod.commitChanges();
    }
  }
}
