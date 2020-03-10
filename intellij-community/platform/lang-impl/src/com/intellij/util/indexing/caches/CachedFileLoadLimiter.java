// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.caches;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

public interface CachedFileLoadLimiter {
  long getMaxLoadedBytes();

  boolean tryReserveBytesForFile(long fileSize, long time, @NotNull TimeUnit timeUnit) throws InterruptedException;

  void releaseFileBytes(long fileSize);
}
