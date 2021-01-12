// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing;

import com.intellij.openapi.util.Computable;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Stream;

abstract class StorageBufferingHandler {
  private final StorageGuard myStorageLock = new StorageGuard();
  private volatile boolean myPreviousDataBufferingState;
  private final Object myBufferingStateUpdateLock = new Object();

  boolean runUpdate(boolean transientInMemoryIndices, @NotNull Computable<Boolean> update) {
    StorageGuard.StorageModeExitHandler storageModeExitHandler = myStorageLock.enter(transientInMemoryIndices);

    if (myPreviousDataBufferingState != transientInMemoryIndices) {
      synchronized (myBufferingStateUpdateLock) {
        if (myPreviousDataBufferingState != transientInMemoryIndices) {
          getIndexes().forEach(index -> {
            assert index != null;
            index.setBufferingEnabled(transientInMemoryIndices);
          });
          myPreviousDataBufferingState = transientInMemoryIndices;
        }
      }
    }

    try {
      return update.compute();
    } finally {
      storageModeExitHandler.leave();
    }
  }

  void resetState() {
    myPreviousDataBufferingState = false;
  }

  @NotNull
  protected abstract Stream<UpdatableIndex<?, ? ,?>> getIndexes();
}
