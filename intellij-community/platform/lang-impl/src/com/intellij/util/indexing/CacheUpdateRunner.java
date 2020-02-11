// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationListener;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ex.ApplicationEx;
import com.intellij.openapi.application.ex.ApplicationManagerEx;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.util.ProgressIndicatorBase;
import com.intellij.openapi.progress.util.ProgressIndicatorUtils;
import com.intellij.openapi.progress.util.ProgressWrapper;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ConcurrencyUtil;
import com.intellij.util.Consumer;
import com.intellij.util.indexing.caches.CachedFileContent;
import com.intellij.util.indexing.caches.CachedFileContentQueue;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

final class CacheUpdateRunner {
  private static final Logger LOG = Logger.getInstance(CacheUpdateRunner.class);
  private static final Key<Boolean> FAILED_TO_INDEX = Key.create("FAILED_TO_INDEX");

  private CacheUpdateRunner() {
  }

  static void processFiles(@NotNull ProgressIndicator indicator,
                          @NotNull Collection<VirtualFile> files,
                          @NotNull Project project,
                          @NotNull Consumer<? super CachedFileContent> processor) {
    ProgressIndicator updaterProgressIndicator = PoweredProgressIndicator.apply(indicator);
    updaterProgressIndicator.checkCanceled();
    final CachedFileContentQueue queue = new CachedFileContentQueue(project, files, updaterProgressIndicator);
    final double total = files.size();
    queue.startLoading();

    updaterProgressIndicator.setIndeterminate(false);

    ProgressUpdater progressUpdater = new ProgressUpdater() {
      final Set<VirtualFile> myFilesBeingProcessed = new THashSet<>();
      final AtomicInteger myNumberOfFilesProcessed = new AtomicInteger();

      @Override
      public void processingStarted(@NotNull VirtualFile virtualFile) {
        updaterProgressIndicator.checkCanceled();
        boolean added;
        synchronized (myFilesBeingProcessed) {
          added = myFilesBeingProcessed.add(virtualFile);
        }
        if (added) {
          updaterProgressIndicator.setFraction(myNumberOfFilesProcessed.incrementAndGet() / total);

          VirtualFile parent = virtualFile.getParent();
          if (parent != null) updaterProgressIndicator.setText2(parent.getPresentableUrl());
        }
      }

      @Override
      public void processingSuccessfullyFinished(@NotNull VirtualFile virtualFile) {
        synchronized (myFilesBeingProcessed) {
          boolean removed = myFilesBeingProcessed.remove(virtualFile);
          assert removed;
        }
      }
    };

    while (!project.isDisposed()) {
      updaterProgressIndicator.checkCanceled();
      if (processSomeFilesWhileUserIsInactive(queue, progressUpdater, updaterProgressIndicator, project, processor)) {
        break;
      }
    }

    if (project.isDisposed()) {
      updaterProgressIndicator.cancel();
      updaterProgressIndicator.checkCanceled();
    }
  }

  private interface ProgressUpdater {
    void processingStarted(@NotNull VirtualFile file);
    void processingSuccessfullyFinished(@NotNull VirtualFile file);
  }

  private static boolean processSomeFilesWhileUserIsInactive(@NotNull CachedFileContentQueue queue,
                                                             @NotNull ProgressUpdater progressUpdater,
                                                             @NotNull ProgressIndicator suspendableIndicator,
                                                             @NotNull Project project,
                                                             @NotNull Consumer<? super CachedFileContent> fileProcessor) {
    final ProgressIndicatorBase innerIndicator = new ProgressIndicatorBase() {
      @Override
      protected boolean isCancelable() {
        return true; // the inner indicator must be always cancelable
      }
    };
    final ApplicationListener canceller = new ApplicationListener() {
      @Override
      public void beforeWriteActionStart(@NotNull Object action) {
        innerIndicator.cancel();
      }
    };
    final Application application = ApplicationManager.getApplication();
    Disposable listenerDisposable = Disposer.newDisposable();
    Ref<Boolean> shouldSetUpListener = new Ref<>(true);
    Runnable setUpListenerRunnable = () -> {
      synchronized (shouldSetUpListener) {
        if (shouldSetUpListener.get()) {
          application.addApplicationListener(canceller, listenerDisposable);
        }
      }
    };
    if (application.isDispatchThread()) {
      setUpListenerRunnable.run();
    } else {
      application.invokeLater(setUpListenerRunnable, ModalityState.any());
    }

    final AtomicBoolean isFinished = new AtomicBoolean();
    try {
      int threadsCount = UnindexedFilesUpdater.getIndexingThreadsNumber();
      if (threadsCount == 1 || application.isWriteAccessAllowed()) {
        Runnable process = createRunnable(project, queue, progressUpdater, suspendableIndicator, innerIndicator, isFinished, fileProcessor);
        ProgressManager.getInstance().runProcess(process, innerIndicator);
      }
      else {
        AtomicBoolean[] finishedRefs = new AtomicBoolean[threadsCount];
        Future<?>[] futures = new Future<?>[threadsCount];
        for (int i = 0; i < threadsCount; i++) {
          AtomicBoolean localFinished = new AtomicBoolean();
          finishedRefs[i] = localFinished;
          Runnable process = createRunnable(project, queue, progressUpdater, suspendableIndicator, innerIndicator, localFinished, fileProcessor);
          futures[i] = application.executeOnPooledThread(process);
        }
        isFinished.set(waitForAll(finishedRefs, futures));
      }
    }
    finally {
      synchronized (shouldSetUpListener) {
        shouldSetUpListener.set(false);
        Disposer.dispose(listenerDisposable);
      }
    }

    return isFinished.get();
  }

  private static boolean waitForAll(AtomicBoolean @NotNull [] finishedRefs, Future<?> @NotNull [] futures) {
    assert !ApplicationManager.getApplication().isWriteAccessAllowed();
    try {
      for (Future<?> future : futures) {
        ProgressIndicatorUtils.awaitWithCheckCanceled(future);
      }

      boolean allFinished = true;
      for (AtomicBoolean ref : finishedRefs) {
        if (!ref.get()) {
          allFinished = false;
          break;
        }
      }
      return allFinished;
    }
    catch (ProcessCanceledException e) {
      throw e;
    }
    catch (Throwable throwable) {
      LOG.error(throwable);
    }
    return false;
  }

  private static Runnable createRunnable(@NotNull Project project,
                                         @NotNull CachedFileContentQueue queue,
                                         @NotNull ProgressUpdater progressUpdater,
                                         @NotNull ProgressIndicator suspendableIndicator,
                                         @NotNull ProgressIndicatorBase innerIndicator,
                                         @NotNull AtomicBoolean isFinished,
                                         @NotNull Consumer<? super CachedFileContent> fileProcessor) {
    Runnable runnable = new UpdateWorker(project, queue, progressUpdater, suspendableIndicator, innerIndicator, isFinished, fileProcessor);
    return ConcurrencyUtil.underThreadNameRunnable("Indexing", runnable);
  }

  private static void handleIndexingException(@NotNull VirtualFile file, @NotNull Throwable e) {
    file.putUserData(FAILED_TO_INDEX, Boolean.TRUE);
    LOG.error("Error while indexing " + file.getPresentableUrl() + "\n" + "To reindex this file IDEA has to be restarted", e);
  }

  private static class UpdateWorker implements Runnable {
    private final Project myProject;
    private final CachedFileContentQueue myQueue;
    private final ProgressUpdater myProgressUpdater;
    private final ProgressIndicator mySuspendableIndicator;
    private final ProgressIndicatorBase myInnerIndicator;
    private final AtomicBoolean myIsFinished;
    private final Consumer<? super CachedFileContent> myFileProcessor;

    UpdateWorker(@NotNull Project project,
                 @NotNull CachedFileContentQueue queue,
                 @NotNull ProgressUpdater progressUpdater,
                 @NotNull ProgressIndicator suspendableIndicator,
                 @NotNull ProgressIndicatorBase innerIndicator,
                 @NotNull AtomicBoolean isFinished,
                 @NotNull Consumer<? super CachedFileContent> fileProcessor) {
      myProject = project;
      myQueue = queue;
      myProgressUpdater = progressUpdater;
      mySuspendableIndicator = suspendableIndicator;
      myInnerIndicator = innerIndicator;
      myIsFinished = isFinished;
      myFileProcessor = fileProcessor;
    }

    @Override
    public void run() {
      while (true) {
        if (myProject.isDisposed() || myInnerIndicator.isCanceled()) {
          return;
        }

        try {
          mySuspendableIndicator.checkCanceled();

          final CachedFileContent fileContent = myQueue.take(myInnerIndicator);
          if (fileContent == null) {
            myIsFinished.set(true);
            return;
          }

          final Runnable action = () -> {
            myInnerIndicator.checkCanceled();
            if (!myProject.isDisposed()) {
              final VirtualFile file = fileContent.getVirtualFile();
              try {
                myProgressUpdater.processingStarted(file);
                if (!file.isDirectory() && !Boolean.TRUE.equals(file.getUserData(FAILED_TO_INDEX))) {
                  myFileProcessor.consume(fileContent);
                }
                myProgressUpdater.processingSuccessfullyFinished(file);
              }
              catch (ProcessCanceledException e) {
                throw e;
              }
              catch (Throwable e) {
                handleIndexingException(file, e);
              }
            }
          };
          try {
            ProgressManager.getInstance().runProcess(() -> {
              // in wait methods we don't want to deadlock by grabbing write lock (or having it in queue) and trying to run read action in separate thread
              ApplicationEx app = ApplicationManagerEx.getApplicationEx();
              if (app.isDisposed() || !app.tryRunReadAction(action)) {
                throw new ProcessCanceledException();
              }
            }, ProgressWrapper.wrap(myInnerIndicator));
          }
          catch (ProcessCanceledException e) {
            myQueue.pushBack(fileContent);
            return;
          }
          finally {
            myQueue.release(fileContent);
          }
        }
        catch (ProcessCanceledException e) {
          return;
        }
      }
    }
  }
}