// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.caches;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public final class UnlimitedSingleCachedFileLoadLimiter implements CachedFileLoadLimiter {

  private boolean myIsReserved;

  private final Lock myLock = new ReentrantLock();

  private final Condition myIsFreeCondition = myLock.newCondition();

  @Override
  public long getMaxLoadedBytes() {
    return Long.MAX_VALUE;
  }

  @Override
  public boolean tryReserveBytesForFile(long fileSize, long time, @NotNull TimeUnit timeUnit) throws InterruptedException {
    myLock.lockInterruptibly();
    try {
      if (myIsReserved) {
        myIsFreeCondition.await(time, timeUnit);
      }
      if (!myIsReserved) {
        myIsReserved = true;
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
      myIsReserved = false;
      myIsFreeCondition.signalAll();
    }
    finally {
      myLock.unlock();
    }
  }
}
