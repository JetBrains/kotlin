// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.caches;

import com.google.common.annotations.VisibleForTesting;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Thread-safe limiter of total amount of bytes loaded into memory by several threads.
 */
public final class MaxTotalSizeCachedFileLoadLimiter implements CachedFileLoadLimiter {

  private final long myMaxLoadedBytes;

  private final Lock myLock = new ReentrantLock();

  private final Condition myCanLoadMoreCondition = myLock.newCondition();

  private long myLoadedBytes;

  public MaxTotalSizeCachedFileLoadLimiter(long maxLoadedBytes) {
    myMaxLoadedBytes = maxLoadedBytes;
  }

  @Override
  public long getMaxLoadedBytes() {
    return myMaxLoadedBytes;
  }

  @VisibleForTesting
  public long getLoadedBytes() {
    myLock.lock();
    try {
      return myLoadedBytes;
    } finally {
      myLock.unlock();
    }
  }

  @Override
  public boolean tryReserveBytesForFile(long fileSize, long time, @NotNull TimeUnit timeUnit) throws InterruptedException {
    myLock.lock();
    try {
      if (myLoadedBytes + fileSize > myMaxLoadedBytes) {
        myCanLoadMoreCondition.await(time, timeUnit);
      }
      if (myLoadedBytes + fileSize <= myMaxLoadedBytes) {
        myLoadedBytes += fileSize;
        return true;
      }
      return false;
    }
    finally {
      myLock.unlock();
    }
  }

  @Override
  public void releaseFileBytes(long fileSize) {
    myLock.lock();
    try {
      myLoadedBytes -= fileSize;
      if (myLoadedBytes < myMaxLoadedBytes) {
        myCanLoadMoreCondition.signalAll();
      }
    }
    finally {
      myLock.unlock();
    }
  }
}
