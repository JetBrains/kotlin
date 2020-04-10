// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing;

import com.intellij.history.LocalHistory;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.application.impl.LaterInvocator;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.DumbModeTask;
import com.intellij.openapi.project.DumbServiceImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.roots.ContentIterator;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.vfs.AsyncFileListener;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileWithId;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.psi.PsiManager;
import com.intellij.util.ConcurrencyUtil;
import com.intellij.util.concurrency.BoundedTaskExecutor;
import com.intellij.util.concurrency.SequentialTaskExecutor;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.IntObjectMap;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

@ApiStatus.Internal
public final class ChangedFilesCollector extends IndexedFilesListener {
  private static final Logger LOG = Logger.getInstance(ChangedFilesCollector.class);

  private final IntObjectMap<VirtualFile> myFilesToUpdate = ContainerUtil.createConcurrentIntObjectMap();
  private final AtomicInteger myProcessedEventIndex = new AtomicInteger();
  private final Phaser myWorkersFinishedSync = new Phaser() {
    @Override
    protected boolean onAdvance(int phase, int registeredParties) {
      return false;
    }
  };

  private final Executor
    myVfsEventsExecutor = SequentialTaskExecutor.createSequentialApplicationPoolExecutor("FileBasedIndex Vfs Event Processor");
  private final AtomicInteger myScheduledVfsEventsWorkers = new AtomicInteger();
  private final FileBasedIndexImpl myManager = (FileBasedIndexImpl)FileBasedIndex.getInstance();

  private final AtomicInteger myUpdatingFiles = new AtomicInteger();

  @Override
  protected void buildIndicesForFileRecursively(@NotNull VirtualFile file, boolean contentChange) {
    FileBasedIndexImpl.cleanProcessedFlag(file);
    if (!contentChange) {
      myUpdatingFiles.incrementAndGet();
    }

    super.buildIndicesForFileRecursively(file, contentChange);

    if (!contentChange) {
      if (myUpdatingFiles.decrementAndGet() == 0) {
        myManager.incrementFilesModCount();
      }
    }
  }

  @Override
  protected void iterateIndexableFiles(@NotNull VirtualFile file, @NotNull ContentIterator iterator) {
    for (IndexableFileSet set : myManager.getIndexableSets()) {
      if (set.isInSet(file)) {
        set.iterateIndexableFilesIn(file, iterator);
      }
    }
  }

  boolean isUpdateInProgress() {
    return myUpdatingFiles.get() > 0;
  }

  void scheduleForUpdate(VirtualFile file) {
    if (!(file instanceof DeletedVirtualFileStub)) {
      IndexableFileSet setForFile = myManager.getIndexableSetForFile(file);
      if (setForFile == null) {
        return;
      }
    }
    final int fileId = Math.abs(FileBasedIndexImpl.getIdMaskingNonIdBasedFile(file));
    final VirtualFile previousVirtualFile = myFilesToUpdate.put(fileId, file);

    if (previousVirtualFile instanceof DeletedVirtualFileStub &&
        !previousVirtualFile.equals(file)) {
      assert ((DeletedVirtualFileStub)previousVirtualFile).getOriginalFile().equals(file);
      ((DeletedVirtualFileStub)previousVirtualFile).setResurrected(true);
      myFilesToUpdate.put(fileId, previousVirtualFile);
    }
  }

  void removeScheduledFileFromUpdate(VirtualFile file) {
    final int fileId = Math.abs(FileBasedIndexImpl.getIdMaskingNonIdBasedFile(file));
    final VirtualFile previousVirtualFile = myFilesToUpdate.remove(fileId);

    if (previousVirtualFile instanceof DeletedVirtualFileStub) {
      assert ((DeletedVirtualFileStub)previousVirtualFile).getOriginalFile().equals(file);
      ((DeletedVirtualFileStub)previousVirtualFile).setResurrected(false);
      myFilesToUpdate.put(fileId, previousVirtualFile);
    }
  }

  void removeFileIdFromFilesScheduledForUpdate(int fileId) {
    myFilesToUpdate.remove(fileId);
  }

  public boolean containsFileId(int fileId) {
    return myFilesToUpdate.containsKey(fileId);
  }

  Stream<VirtualFile> getFilesToUpdate() {
    return myFilesToUpdate.values().stream();
  }

  Collection<VirtualFile> getAllFilesToUpdate() {
    ensureUpToDate();
    if (myFilesToUpdate.isEmpty()) {
      return Collections.emptyList();
    }
    return new ArrayList<>(myFilesToUpdate.values());
  }

  // it's important here to don't load any extension here, so we don't check scopes.
  Collection<VirtualFile> getAllPossibleFilesToUpdate() {

    ReadAction.run(() -> {
      processFilesInReadAction(info -> {
        myFilesToUpdate.put(info.getFileId(),
                            info.isFileRemoved() ? new DeletedVirtualFileStub(((VirtualFileWithId)info.getFile())) : info.getFile());
        return true;
      });
    });

    return new ArrayList<>(myFilesToUpdate.values());
  }

  void clearFilesToUpdate() {
    myFilesToUpdate.clear();
  }

  @Override
  @NotNull
  public AsyncFileListener.ChangeApplier prepareChange(@NotNull List<? extends VFileEvent> events) {
    boolean shouldCleanup = ContainerUtil.exists(events, ChangedFilesCollector::memoryStorageCleaningNeeded);
    ChangeApplier superApplier = super.prepareChange(events);

    return new ChangeApplier() {
      @Override
      public void beforeVfsChange() {
        if (shouldCleanup) {
          myManager.cleanupMemoryStorage(false);
        }
        superApplier.beforeVfsChange();
      }

      @Override
      public void afterVfsChange() {
        superApplier.afterVfsChange();
        RegisteredIndexes registeredIndexes = myManager.getRegisteredIndexes();
        if (registeredIndexes != null && registeredIndexes.isInitialized()) ensureUpToDateAsync();
      }
    };
  }

  private static boolean memoryStorageCleaningNeeded(@NotNull VFileEvent event) {
    Object requestor = event.getRequestor();
    return requestor instanceof FileDocumentManager ||
           requestor instanceof PsiManager ||
           requestor == LocalHistory.VFS_EVENT_REQUESTOR;
  }

  boolean isScheduledForUpdate(VirtualFile file) {
    return myFilesToUpdate.containsKey(Math.abs(FileBasedIndexImpl.getIdMaskingNonIdBasedFile(file)));
  }

  void ensureUpToDate() {
    if (!FileBasedIndexImpl.isUpToDateCheckEnabled()) {
      return;
    }
    //assert ApplicationManager.getApplication().isReadAccessAllowed() || ShutDownTracker.isShutdownHookRunning();
    myManager.waitUntilIndicesAreInitialized();

    if (ApplicationManager.getApplication().isReadAccessAllowed()) {
      processFilesToUpdateInReadAction();
    }
    else {
      processFilesInReadActionWithYieldingToWriteAction();
    }
  }

  void ensureUpToDateAsync() {
    if (getEventMerger().getApproximateChangesCount() >= 20 && myScheduledVfsEventsWorkers.compareAndSet(0,1)) {
      myVfsEventsExecutor.execute(() -> {
        try {
          processFilesInReadActionWithYieldingToWriteAction();
        }
        finally {
          myScheduledVfsEventsWorkers.decrementAndGet();
        }
      });

      if (Registry.is("try.starting.dumb.mode.where.many.files.changed")) {
        Runnable startDumbMode = () -> {
          for (Project project : ProjectManager.getInstance().getOpenProjects()) {
            DumbServiceImpl dumbService = DumbServiceImpl.getInstance(project);
            DumbModeTask task = FileBasedIndexProjectHandler.createChangedFilesIndexingTask(project);

            if (task != null) {
              dumbService.queueTask(task);
            }
          }
        };

        Application app = ApplicationManager.getApplication();
        if (!app.isHeadlessEnvironment()  /*avoid synchronous ensureUpToDate to prevent deadlock*/ &&
            app.isDispatchThread() &&
            !LaterInvocator.isInModalContext()) {
          startDumbMode.run();
        }
        else {
          app.invokeLater(startDumbMode, ModalityState.NON_MODAL);
        }
      }
    }
  }

  private void processFilesToUpdateInReadAction() {
    processFilesInReadAction(info -> {
      int fileId = info.getFileId();
      VirtualFile file = info.getFile();
      if (info.isTransientStateChanged()) myManager.doTransientStateChangeForFile(fileId, file);
      if (info.isBeforeContentChanged()) myManager.doInvalidateIndicesForFile(fileId, file, true);
      if (info.isContentChanged()) myManager.scheduleFileForIndexing(fileId, file, true);
      if (info.isFileRemoved()) myManager.doInvalidateIndicesForFile(fileId, file, false);
      if (info.isFileAdded()) myManager.scheduleFileForIndexing(fileId, file, false);
      return true;
    });
  }

  private void processFilesInReadAction(@NotNull VfsEventsMerger.VfsEventProcessor processor) {
    assert ApplicationManager.getApplication().isReadAccessAllowed(); // no vfs events -> event processing code can finish

    int publishedEventIndex = getEventMerger().getPublishedEventIndex();
    int processedEventIndex = myProcessedEventIndex.get();
    if (processedEventIndex == publishedEventIndex) {
      return;
    }

    myWorkersFinishedSync.register();
    int phase = myWorkersFinishedSync.getPhase();
    try {
      getEventMerger().processChanges(info ->
        ConcurrencyUtil.withLock(myManager.myWriteLock, () -> {
          try {
            ProgressManager.getInstance().executeNonCancelableSection(() -> {
              processor.process(info);
            });
          }
          finally {
            IndexingStamp.flushCache(info.getFileId());
          }
          return true;
        })
      );
    }
    finally {
      myWorkersFinishedSync.arriveAndDeregister();
    }

    try {
      myWorkersFinishedSync.awaitAdvance(phase);
    } catch (RejectedExecutionException e) {
      LOG.warn(e);
      throw new ProcessCanceledException(e);
    }

    if (getEventMerger().getPublishedEventIndex() == publishedEventIndex) {
      myProcessedEventIndex.compareAndSet(processedEventIndex, publishedEventIndex);
    }
  }

  private void processFilesInReadActionWithYieldingToWriteAction() {
    while (getEventMerger().hasChanges()) {
      ReadAction.nonBlocking(() -> processFilesToUpdateInReadAction()).executeSynchronously();
    }
  }

  @TestOnly
  public void waitForVfsEventsExecuted(long timeout, @NotNull TimeUnit unit) throws Exception {
    ApplicationManager.getApplication().assertIsDispatchThread();
    long deadline = System.nanoTime() + unit.toNanos(timeout);
    while (System.nanoTime() < deadline) {
      try {
        ((BoundedTaskExecutor)myVfsEventsExecutor).waitAllTasksExecuted(100, TimeUnit.MILLISECONDS);
        return;
      }
      catch (TimeoutException e) {
        UIUtil.dispatchAllInvocationEvents();
      }
    }
  }
}
