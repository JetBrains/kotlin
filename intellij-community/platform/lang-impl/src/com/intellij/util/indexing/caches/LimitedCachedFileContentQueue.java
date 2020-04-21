// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.caches;

import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ExceptionUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread-safe queue that loads file contents.
 * Allows to limit total amount of bytes loaded into memory by means of {@link CachedFileLoadLimiter}.
 */
public final class LimitedCachedFileContentQueue implements CachedFileContentQueue {
  private final BlockingQueue<VirtualFile> myFilesQueue;
  private final CachedFileLoadLimiter myFileLoadLimiter;
  private final CachedFileContentLoader myContentLoader;
  private final AtomicInteger myTotalNumberOfFilesToBeProcessed;
  private final AtomicInteger myNumberOfProcessedFiles = new AtomicInteger();
  private final BlockingQueue<LoadedCachedFileContentToken> myPushedBackTokens = new LinkedBlockingQueue<>();
  private final AtomicLong myTotalReservedBytes = new AtomicLong();
  private final boolean myIsAppendable;

  private LimitedCachedFileContentQueue(@NotNull BlockingQueue<VirtualFile> filesQueue,
                                        @NotNull CachedFileLoadLimiter fileLoadLimiter,
                                        @NotNull CachedFileContentLoader contentLoader,
                                        boolean isAppendable) {
    myFilesQueue = filesQueue;
    myFileLoadLimiter = fileLoadLimiter;
    myContentLoader = contentLoader;
    myIsAppendable = isAppendable;
    myTotalNumberOfFilesToBeProcessed = new AtomicInteger(filesQueue.size());
  }

  @NotNull
  public static LimitedCachedFileContentQueue createNonAppendableForFiles(@NotNull Collection<? extends VirtualFile> files,
                                                                          @NotNull CachedFileLoadLimiter fileLoadLimiter,
                                                                          @NotNull CachedFileContentLoader contentLoader) {
    return new LimitedCachedFileContentQueue(new ArrayBlockingQueue<>(files.size(), false, files), fileLoadLimiter, contentLoader,
                                             false);
  }

  @NotNull
  public static LimitedCachedFileContentQueue createEmptyAppendable(@NotNull CachedFileLoadLimiter fileLoadLimiter,
                                                                    @NotNull CachedFileContentLoader contentLoader) {
    return new LimitedCachedFileContentQueue(new LinkedBlockingQueue<>(), fileLoadLimiter, contentLoader, true);
  }

  public void addFileToLoad(@NotNull VirtualFile file) {
    if (!myIsAppendable) {
      throw new IllegalStateException("No files can be added to this queue");
    }
    myTotalNumberOfFilesToBeProcessed.incrementAndGet();
    myFilesQueue.offer(file);
  }

  @Override
  @Nullable
  public CachedFileContentToken loadNextContent(@NotNull ProgressIndicator indicator) throws FailedToLoadContentException,
                                                                                             TooLargeContentException,
                                                                                             ProcessCanceledException {
    while (true) {
      indicator.checkCanceled();

      // Try to pull a pushed-back content.
      LoadedCachedFileContentToken pushedBackToken = myPushedBackTokens.poll();
      if (pushedBackToken != null) {
        return pushedBackToken;
      }

      // Try to load the next file from the queue.
      VirtualFile file = myFilesQueue.poll();
      if (file == null) {
        if (areAllFilesProcessed()) {
          return null;
        }

        // There are still files being processed in other threads, some of which may be pushed back.
        try {
          LoadedCachedFileContentToken pushedBackToken2 = myPushedBackTokens.poll(10, TimeUnit.MILLISECONDS);
          if (pushedBackToken2 != null) {
            return pushedBackToken2;
          }
          continue;
        }
        catch (InterruptedException e) {
          throw new ProcessCanceledException();
        }
      }

      long fileSize = file.getLength();
      if (fileSize > myFileLoadLimiter.getMaxLoadedBytes()) {
        // Consider such too large files as processed.
        myNumberOfProcessedFiles.incrementAndGet();
        throw new TooLargeContentException(file, fileSize);
      }

      boolean reserved;
      try {
        reserved = reserveBytesForFile(fileSize);
      }
      catch (Throwable e) {
        // Unexpected exception. Pretend the file has not been processed. Return file to the queue. No bytes have been reserved.
        myFilesQueue.offer(file);
        ExceptionUtil.rethrow(e);
        throw new AssertionError("Cannot happen");
      }

      if (reserved) {
        CachedFileContent content;
        try {
          content = myContentLoader.loadContent(file);
        }
        catch (ProcessCanceledException e) {
          releaseReservedBytes(fileSize);
          // Return file to the queue. It will be processed later.
          myFilesQueue.offer(file);
          throw e;
        }
        catch (Throwable e) {
          // Consider files with failed to be loaded content as processed.
          releaseReservedBytesAndIncrementProcessed(fileSize);

          //noinspection InstanceofCatchParameter
          if (e instanceof FailedToLoadContentException) {
            throw (FailedToLoadContentException)e;
          }
          //noinspection InstanceofCatchParameter
          if (e instanceof TooLargeContentException) {
            throw (TooLargeContentException)e;
          }
          throw new FailedToLoadContentException(file, e);
        }
        return new LoadedCachedFileContentToken(fileSize, content);
      }
      else {
        // Return file to the queue. No bytes have been reserved.
        myFilesQueue.offer(file);
      }
    }
  }

  private boolean areAllFilesProcessed() {
    /*
    This code makes sure that we don't return `null` from `loadNextContent` until all files have been processed,
    including those that can be potentially pushed back (see IDEA-238381).
      */
    int numberOfProcessedFiles = myNumberOfProcessedFiles.get();
    if (numberOfProcessedFiles == myTotalNumberOfFilesToBeProcessed.get()) {
      if (myTotalReservedBytes.get() != 0) {
        // Some files might have been added to the queue for loading. If so, newly reserved bytes are OK.
        if (numberOfProcessedFiles == myTotalNumberOfFilesToBeProcessed.get()) {
          throw new RuntimeException("Implementation of LimitedCachedFileContentQueue is incorrect: some tokens have not released reserved bytes");
        }
        return false;
      }
      return true;
    }
    return false;
  }

  private boolean reserveBytesForFile(long fileSize) throws InterruptedException {
    boolean reserved = myFileLoadLimiter.tryReserveBytesForFile(fileSize, 10, TimeUnit.MILLISECONDS);
    if (reserved) {
      myTotalReservedBytes.addAndGet(fileSize);
    }
    return reserved;
  }

  private void releaseReservedBytes(long fileSize) {
    myFileLoadLimiter.releaseFileBytes(fileSize);
    myTotalReservedBytes.addAndGet(-fileSize);
  }

  // Order is important: firstly release bytes, then increment "processed" counter.
  // So that code in [areAllFilesProcessed] does not have a race.
  private void releaseReservedBytesAndIncrementProcessed(long fileSize) {
    releaseReservedBytes(fileSize);
    myNumberOfProcessedFiles.incrementAndGet();
  }

  private final class LoadedCachedFileContentToken implements CachedFileContentToken {
    private final long myFileSize;
    private final CachedFileContent myContent;

    private LoadedCachedFileContentToken(long fileSize, CachedFileContent content) {
      myFileSize = fileSize;
      myContent = content;
    }

    @Override
    @NotNull
    public CachedFileContent getContent() {
      return myContent;
    }

    @Override
    public void release() {
      releaseReservedBytesAndIncrementProcessed(myFileSize);
    }

    @Override
    public void pushBack() {
      // Push the file to the pushed-backs queue. Bytes reserved for the file in [myFileLoadLimiter] are not released yet.
      myPushedBackTokens.offer(this);
    }
  }
}