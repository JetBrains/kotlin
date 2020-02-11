// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.caches;

import com.intellij.openapi.application.AccessToken;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectLocator;
import com.intellij.openapi.vfs.InvalidVirtualFileAccessException;
import com.intellij.openapi.vfs.VFileProperty;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.concurrency.SequentialTaskExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.Deque;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author peter
 */
public final class CachedFileContentQueue {
  private static final Logger LOG = Logger.getInstance(CachedFileContentQueue.class);

  private static final long MAX_SIZE_OF_BYTES_IN_QUEUE = 1024 * 1024;
  private static final long PROCESSED_FILE_BYTES_THRESHOLD = 1024 * 1024 * 3;
  private static final long LARGE_SIZE_REQUEST_THRESHOLD = PROCESSED_FILE_BYTES_THRESHOLD - 1024 * 300; // 300k for other threads

  private static final Executor ourExecutor = SequentialTaskExecutor.createSequentialApplicationPoolExecutor("FileContentQueue Pool");

  // Unbounded (!)
  private final LinkedBlockingDeque<CachedFileContent> myLoadedContents = new LinkedBlockingDeque<>();
  private final AtomicInteger myContentsToLoad = new AtomicInteger();

  private final AtomicLong myLoadedBytesInQueue = new AtomicLong();
  private static final Object ourProceedWithLoadingLock = new Object();
  @NotNull private final Project myProject;

  private volatile long myBytesBeingProcessed;
  private volatile boolean myLargeSizeRequested;
  private final Object myProceedWithProcessingLock = new Object();
  private final BlockingQueue<VirtualFile> myFilesQueue;
  private final ProgressIndicator myProgressIndicator;
  private static final Deque<CachedFileContentQueue> ourContentLoadingQueues = new LinkedBlockingDeque<>();

  public CachedFileContentQueue(@NotNull Project project,
                                @NotNull Collection<? extends VirtualFile> files,
                                @NotNull final ProgressIndicator indicator) {
    myProject = project;
    int numberOfFiles = files.size();
    myContentsToLoad.set(numberOfFiles);
    // ABQ is more memory efficient for significant number of files (e.g. 500K)
    myFilesQueue = numberOfFiles > 0 ? new ArrayBlockingQueue<>(numberOfFiles, false, files) : null;
    myProgressIndicator = indicator;
  }

  public void startLoading() {
    if (myContentsToLoad.get() == 0) return;

    ourContentLoadingQueues.addLast(this);

    Runnable task = () -> {
      CachedFileContentQueue contentQueue = ourContentLoadingQueues.pollFirst();

      while (contentQueue != null) {
        PreloadState preloadState = contentQueue.preloadNextContent();

        if (preloadState == PreloadState.PRELOADED_SUCCESSFULLY ||
            preloadState == PreloadState.TOO_MUCH_DATA_PRELOADED) {
          ourContentLoadingQueues.addLast(contentQueue);
        }
        contentQueue = ourContentLoadingQueues.pollFirst();
      }
    };
    ourExecutor.execute(task);
  }

  private enum PreloadState {
    TOO_MUCH_DATA_PRELOADED, PRELOADED_SUCCESSFULLY, CANCELLED_OR_FINISHED
  }

  private PreloadState preloadNextContent() {
    try {
      if (myLoadedBytesInQueue.get() > MAX_SIZE_OF_BYTES_IN_QUEUE) {
        // wait a little for indexer threads to consume content, they will awake us earlier once we can proceed
        synchronized (ourProceedWithLoadingLock) {
          //noinspection WaitNotInLoop
          ourProceedWithLoadingLock.wait(300);
        }
        myProgressIndicator.checkCanceled();
        return PreloadState.TOO_MUCH_DATA_PRELOADED;
      }
    } catch (InterruptedException e) {
      LOG.error(e);
    }
    catch (ProcessCanceledException pce) {
      return PreloadState.CANCELLED_OR_FINISHED;
    }

    if (myProgressIndicator.isCanceled()) return PreloadState.CANCELLED_OR_FINISHED;
    return loadNextContent() ? PreloadState.PRELOADED_SUCCESSFULLY : PreloadState.CANCELLED_OR_FINISHED;
  }

  private boolean loadNextContent() {
    // Contract: if file is taken from myFilesQueue then it will be loaded to myLoadedContents and myContentsToLoad will be decremented
    VirtualFile file = myFilesQueue.poll();
    if (file == null) return false;

    try {
      CachedFileContent content = new CachedFileContent(file);
      if (!isValidFile(file) || !doLoadContent(content)) {
        content.setEmptyContent();
      }
      myLoadedContents.offer(content);
      return true;
    }
    finally {
      myContentsToLoad.addAndGet(-1);
    }
  }

  private static boolean isValidFile(@NotNull VirtualFile file) {
    return file.isValid() && !file.isDirectory() && !file.is(VFileProperty.SPECIAL) && !VfsUtilCore.isBrokenLink(file);
  }

  @SuppressWarnings("InstanceofCatchParameter")
  private boolean doLoadContent(@NotNull CachedFileContent content) {
    final long contentLength = content.getLength();

    try {
      myLoadedBytesInQueue.addAndGet(contentLength);

      // Reads the content bytes and caches them.
      // hint at the current project to avoid expensive read action in ProjectLocatorImpl
      try (AccessToken ignored = ProjectLocator.runWithPreferredProject(content.getVirtualFile(), myProject)) {
        content.getBytes();
      }
      return true;
    }
    catch (Throwable e) {
      myLoadedBytesInQueue.addAndGet(-contentLength); // revert size counter

      if (e instanceof IOException || e instanceof InvalidVirtualFileAccessException) {
        if (e instanceof FileNotFoundException) {
          LOG.debug(e); // it is possible to not observe file system change until refresh finish, we handle missed file properly anyway
        } else {
          LOG.info(e);
        }
      }
      else {
        LOG.error(e);
      }

      return false;
    }
  }

  @Nullable
  public CachedFileContent take(@NotNull ProgressIndicator indicator) throws ProcessCanceledException {
    final CachedFileContent content = doTake(indicator);
    if (content == null) {
      return null;
    }
    final long length = content.getLength();
    while (true) {
      try {
        indicator.checkCanceled();
      }
      catch (ProcessCanceledException e) {
        pushBack(content);
        throw e;
      }

      synchronized (myProceedWithProcessingLock) {
        final boolean requestingLargeSize = length > LARGE_SIZE_REQUEST_THRESHOLD;
        if (requestingLargeSize) {
          myLargeSizeRequested = true;
        }
        try {
          if (myLargeSizeRequested && !requestingLargeSize ||
              myBytesBeingProcessed + length > Math.max(PROCESSED_FILE_BYTES_THRESHOLD, length)) {
            myProceedWithProcessingLock.wait(300);
          }
          else {
            myBytesBeingProcessed += length;
            if (requestingLargeSize) {
              myLargeSizeRequested = false;
            }
            return content;
          }
        }
        catch (InterruptedException ignore) {
        }
      }
    }
  }

  @Nullable
  private CachedFileContent doTake(ProgressIndicator indicator) {
    CachedFileContent result = null;
    boolean waitForContentsToBeLoaded = false;

    while (result == null) {
      indicator.checkCanceled();

      final int remainingContentsToLoad = myContentsToLoad.get();
      result = pollLoadedContent(waitForContentsToBeLoaded && remainingContentsToLoad > 0);

      if (result == null) {  // no loaded contents by other threads
        if (remainingContentsToLoad == 0) return null; // no items to load

        if (!loadNextContent()) { // attempt to eagerly load content failed
          // last remaining contents are loaded by other threads, use timed poll for results
          waitForContentsToBeLoaded = true;
        }
      }
    }

    long loadedBytesInQueueNow = myLoadedBytesInQueue.addAndGet(-result.getLength());
    if (loadedBytesInQueueNow < MAX_SIZE_OF_BYTES_IN_QUEUE) {
      // nudge content preloader to proceed
      synchronized (ourProceedWithLoadingLock) {
        // we actually ask only content loading thread to proceed, so there should not be much difference with plain notify
        ourProceedWithLoadingLock.notifyAll();
      }
    }

    return result;
  }

  @Nullable
  private CachedFileContent pollLoadedContent(boolean waitForContentsToBeLoaded) {
    if (waitForContentsToBeLoaded) {
      try {
        return myLoadedContents.poll(50, TimeUnit.MILLISECONDS);
      } catch(InterruptedException ex) { throw new RuntimeException(ex); }
    } else {
      return myLoadedContents.poll();
    }
  }

  public void release(@NotNull CachedFileContent content) {
    synchronized (myProceedWithProcessingLock) {
      myBytesBeingProcessed -= content.getLength();
      myProceedWithProcessingLock.notifyAll(); // ask all sleeping threads to proceed, there can be more than one of them
    }
  }

  public void pushBack(@NotNull CachedFileContent content) {
    myLoadedBytesInQueue.addAndGet(content.getLength());
    myLoadedContents.addFirst(content);
  }
}