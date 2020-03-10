// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.caches;

import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread-safe queue that loads file contents.
 * Allows to limit total amount of bytes loaded into memory by means of {@link CachedFileLoadLimiter}.
 */
public final class LimitedCachedFileContentQueue implements CachedFileContentQueue {
  private final BlockingQueue<VirtualFile> myFilesQueue;
  private final CachedFileLoadLimiter myFileLoadLimiter;
  private final CachedFileContentLoader myContentLoader;
  private final AtomicInteger myFilesBeingProcessed = new AtomicInteger();
  private final BlockingQueue<LoadedCachedFileContentToken> myPushedBackTokens = new LinkedBlockingQueue<>();
  private final boolean myIsAppendable;

  private LimitedCachedFileContentQueue(@NotNull BlockingQueue<VirtualFile> filesQueue,
                                        @NotNull CachedFileLoadLimiter fileLoadLimiter,
                                        @NotNull CachedFileContentLoader contentLoader,
                                        boolean isAppendable) {
    myFilesQueue = filesQueue;
    myFileLoadLimiter = fileLoadLimiter;
    myContentLoader = contentLoader;
    myIsAppendable = isAppendable;
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
        if (myFilesBeingProcessed.get() == 0) {
          // All files have been loaded and processed.
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
        throw new TooLargeContentException(file, fileSize);
      }

      boolean reserved;
      try {
        reserved = myFileLoadLimiter.tryReserveBytesForFile(fileSize, 10, TimeUnit.MILLISECONDS);
      }
      catch (Throwable e) {
        // Return file to the queue. No bytes have been reserved.
        myFilesQueue.offer(file);
        throw new ProcessCanceledException(e);
      }

      if (reserved) {
        CachedFileContent content;
        try {
          content = myContentLoader.loadContent(file);
        }
        catch (ProcessCanceledException e) {
          // Return file to the queue.
          myFilesQueue.offer(file);
          // Release bytes reserved for this file.
          myFileLoadLimiter.releaseFileBytes(fileSize);
          throw e;
        }
        catch (Throwable e) {
          // Release bytes reserved for this file.
          myFileLoadLimiter.releaseFileBytes(fileSize);

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
        myFilesBeingProcessed.incrementAndGet();
        return new LoadedCachedFileContentToken(content);
      }
      else {
        // Return file to the queue. No bytes have been reserved.
        myFilesQueue.offer(file);
      }
    }
  }

  private final class LoadedCachedFileContentToken implements CachedFileContentToken {
    private final CachedFileContent myContent;

    private LoadedCachedFileContentToken(CachedFileContent content) {myContent = content;}

    @Override
    @NotNull
    public CachedFileContent getContent() {
      return myContent;
    }

    @Override
    public void release() {
      myFilesBeingProcessed.decrementAndGet();

      // Release reserved bytes.
      myFileLoadLimiter.releaseFileBytes(myContent.getLength());
    }

    @Override
    public void pushBack() {
      // Push the file to the pushed-backs queue. Bytes reserved for the file in [myFileLoadLimiter] are not released yet.
      myPushedBackTokens.offer(this);
    }
  }
}