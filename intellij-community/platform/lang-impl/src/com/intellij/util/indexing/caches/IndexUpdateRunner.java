// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.caches;

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
import com.intellij.util.indexing.UnindexedFilesUpdater;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public final class IndexUpdateRunner {
  private static final Logger LOG = Logger.getInstance(IndexUpdateRunner.class);
  private static final Key<Boolean> FAILED_TO_INDEX = Key.create("FAILED_TO_INDEX");

  private IndexUpdateRunner() {
  }

  public static void processFiles(@NotNull ProgressIndicator indicator,
                                  @NotNull Collection<VirtualFile> files,
                                  @NotNull Project project,
                                  @NotNull Consumer<? super CachedFileContent> processor) {
    indicator.checkCanceled();
    final CachedFileContentQueue queue = new CachedFileContentQueue(project, files, indicator);
    final double total = files.size();
    queue.startLoading();

    indicator.setIndeterminate(false);

    ProgressUpdater progressUpdater = new ProgressUpdater() {
      final Set<VirtualFile> myFilesBeingProcessed = new THashSet<>();
      final AtomicInteger myNumberOfFilesProcessed = new AtomicInteger();

      @Override
      public void processingStarted(@NotNull VirtualFile virtualFile) {
        indicator.checkCanceled();
        boolean added;
        synchronized (myFilesBeingProcessed) {
          added = myFilesBeingProcessed.add(virtualFile);
        }
        if (added) {
          indicator.setFraction(myNumberOfFilesProcessed.incrementAndGet() / total);
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
      indicator.checkCanceled();
      if (processSomeFilesWhileUserIsInactive(queue, progressUpdater, indicator, project, processor)) {
        break;
      }
    }

    if (project.isDisposed()) {
      indicator.cancel();
      indicator.checkCanceled();
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
      int threadsCount = UnindexedFilesUpdater.getNumberOfIndexingThreads();
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
    return ConcurrencyUtil.underThreadNameRunnable("Indexing", () -> {
      while (true) {
        if (project.isDisposed() || innerIndicator.isCanceled()) {
          return;
        }

        try {
          suspendableIndicator.checkCanceled();

          final CachedFileContent fileContent = queue.take(innerIndicator);
          if (fileContent == null) {
            isFinished.set(true);
            return;
          }

          final Runnable action = () -> {
            innerIndicator.checkCanceled();
            if (!project.isDisposed()) {
              final VirtualFile file = fileContent.getVirtualFile();
              try {
                progressUpdater.processingStarted(file);
                if (!file.isDirectory() && !Boolean.TRUE.equals(file.getUserData(FAILED_TO_INDEX))) {
                  fileProcessor.consume(fileContent);
                }
                progressUpdater.processingSuccessfullyFinished(file);
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
            }, ProgressWrapper.wrap(innerIndicator));
          }
          catch (ProcessCanceledException e) {
            queue.pushBack(fileContent);
            return;
          }
          finally {
            queue.release(fileContent);
          }
        }
        catch (ProcessCanceledException e) {
          return;
        }
      }
    });
  }

  private static void handleIndexingException(@NotNull VirtualFile file, @NotNull Throwable e) {
    file.putUserData(FAILED_TO_INDEX, Boolean.TRUE);
    LOG.error("Error while indexing " + file.getPresentableUrl() + "\n" + "To reindex this file IDEA has to be restarted", e);
  }
}