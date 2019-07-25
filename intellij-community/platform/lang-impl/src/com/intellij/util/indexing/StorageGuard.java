/*
 * Copyright 2000-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.util.indexing;

import org.jetbrains.annotations.NotNull;

@SuppressWarnings({"WhileLoopSpinsOnField", "SynchronizeOnThis"})
class StorageGuard {
  private int myHolds;
  private int myWaiters;

  public interface StorageModeExitHandler {
    void leave();
  }

  private final StorageModeExitHandler myTrueStorageModeExitHandler = new StorageModeExitHandler() {
    @Override
    public void leave() {
      StorageGuard.this.leave(true);
    }
  };
  private final StorageModeExitHandler myFalseStorageModeExitHandler = new StorageModeExitHandler() {
    @Override
    public void leave() {
      StorageGuard.this.leave(false);
    }
  };

  @NotNull
  synchronized StorageModeExitHandler enter(boolean mode) {
    if (mode) {
      while (myHolds < 0) {
        doWait();
      }
      myHolds++;
      return myTrueStorageModeExitHandler;
    }
    else {
      while (myHolds > 0) {
        doWait();
      }
      myHolds--;
      return myFalseStorageModeExitHandler;
    }
  }

  private void doWait() {
    try {
      ++myWaiters;
      wait();
    }
    catch (InterruptedException ignored) {
    }
    finally {
      --myWaiters;
    }
  }

  private synchronized void leave(boolean mode) {
    myHolds += mode ? -1 : 1;
    if (myHolds == 0 && myWaiters > 0) {
      notifyAll();
    }
  }
}
