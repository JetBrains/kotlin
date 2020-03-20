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
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.ArchiveFileSystem;
import com.intellij.util.ConcurrencyUtil;
import com.intellij.util.Consumer;
import com.intellij.util.PathUtil;
import com.intellij.util.indexing.UnindexedFilesUpdater;
import com.intellij.util.progress.SubTaskProgressIndicator;
import gnu.trove.THashSet;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@ApiStatus.Internal
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

          String presentableLocation = getPresentableLocationBeingIndexed(project, virtualFile);
          if (indicator instanceof SubTaskProgressIndicator) {
            indicator.setText(presentableLocation);
          } else {
            indicator.setText2(presentableLocation);
          }
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

  @NotNull
  private static String getPresentableLocationBeingIndexed(@NotNull Project project, @NotNull VirtualFile file) {
    VirtualFile actualFile;
    if (file.getFileSystem() instanceof ArchiveFileSystem) {
      actualFile = VfsUtil.getLocalFile(file);
    } else {
      actualFile = file.getParent() != null ? file.getParent() : file;
    }
    return FileUtil.toSystemDependentName(getProjectRelativeOrAbsolutePath(project, actualFile));
  }

  @NotNull
  private static String getProjectRelativeOrAbsolutePath(@NotNull Project project, @NotNull VirtualFile file) {
    String projectBase = project.getBasePath();
    if (StringUtil.isNotEmpty(projectBase)) {
      String filePath = file.getPath();
      if (FileUtil.isAncestor(projectBase, filePath, true)) {
        String projectDirName = PathUtil.getFileName(projectBase);
        String relativePath = FileUtil.getRelativePath(projectBase, filePath, '/');
        if (StringUtil.isNotEmpty(projectDirName) && StringUtil.isNotEmpty(relativePath)) {
          return projectDirName + "/" + relativePath;
        }
      }
    }
    return file.getPath();
  }

  private static boolean processSomeFilesWhileUserIsInactive(@NotNull CachedFileContentQueue queue,
                                                             @NotNull ProgressUpdater progressUpdater,
                                                             @NotNull ProgressIndicator indexingIndicator,
                                                             @NotNull Project project,
                                                             @NotNull Consumer<? super CachedFileContent> fileProcessor) {
    final ProgressIndicatorBase writeActionIndicator = new ProgressIndicatorBase() {
      @Override
      protected boolean isCancelable() {
        return true; // the inner indicator must be always cancelable
      }
    };
    final ApplicationListener canceller = new ApplicationListener() {
      @Override
      public void beforeWriteActionStart(@NotNull Object action) {
        writeActionIndicator.cancel();
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
        Runnable process = createRunnable(project, queue, progressUpdater, indexingIndicator, writeActionIndicator, isFinished, fileProcessor);
        ProgressManager.getInstance().runProcess(process, writeActionIndicator);
      }
      else {
        AtomicBoolean[] finishedRefs = new AtomicBoolean[threadsCount];
        Future<?>[] futures = new Future<?>[threadsCount];
        for (int i = 0; i < threadsCount; i++) {
          AtomicBoolean localFinished = new AtomicBoolean();
          finishedRefs[i] = localFinished;
          Runnable process = createRunnable(project, queue, progressUpdater, indexingIndicator, writeActionIndicator, localFinished, fileProcessor);
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
                                         @NotNull ProgressIndicator indexingIndicator,
                                         @NotNull ProgressIndicatorBase writeActionIndicator,
                                         @NotNull AtomicBoolean isFinished,
                                         @NotNull Consumer<? super CachedFileContent> fileProcessor) {
    return ConcurrencyUtil.underThreadNameRunnable("Indexing", () -> {
      while (true) {
        if (project.isDisposed() || writeActionIndicator.isCanceled()) {
          return;
        }

        try {
          indexingIndicator.checkCanceled();

          final CachedFileContent fileContent = queue.take(writeActionIndicator);
          if (fileContent == null) {
            isFinished.set(true);
            return;
          }

          final Runnable action = () -> {
            writeActionIndicator.checkCanceled();
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
            }, ProgressWrapper.wrap(writeActionIndicator));
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