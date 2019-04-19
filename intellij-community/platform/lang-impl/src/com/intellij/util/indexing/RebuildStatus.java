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

import com.intellij.openapi.progress.ProgressManager;
import com.intellij.util.ThrowableRunnable;
import com.intellij.util.TimeoutUtil;
import com.intellij.util.containers.ContainerUtil;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author peter
 */
enum RebuildStatus {
  OK,
  REQUIRES_REBUILD,
  DOING_REBUILD;

  private static final Map<ID<?, ?>, AtomicReference<RebuildStatus>> ourRebuildStatus = ContainerUtil.newTroveMap();

  static void registerIndex(ID<?, ?> indexId) {
    ourRebuildStatus.put(indexId, new AtomicReference<>(OK));
  }

  static boolean isOk(ID<?, ?> indexId) {
    AtomicReference<RebuildStatus> rebuildStatus = ourRebuildStatus.get(indexId);
    return rebuildStatus != null && rebuildStatus.get() == OK;
  }

  static boolean requestRebuild(ID<?, ?> indexId) {
    return ourRebuildStatus.get(indexId).compareAndSet(OK, REQUIRES_REBUILD);
  }

  static void clearIndexIfNecessary(ID<?, ?> indexId, ThrowableRunnable<StorageException> clearAction) throws StorageException {
    AtomicReference<RebuildStatus> rebuildStatus = ourRebuildStatus.get(indexId);
    if (rebuildStatus == null) {
      throw new StorageException("Problem updating " + indexId);
    }

    while (rebuildStatus.get() != OK) {
      if (rebuildStatus.compareAndSet(REQUIRES_REBUILD, DOING_REBUILD)) {
        try {
          clearAction.run();
          if (!rebuildStatus.compareAndSet(DOING_REBUILD, OK)) {
            throw new AssertionError("Unexpected status " + rebuildStatus.get());
          }
          continue;
        }
        catch (Throwable e) {
          rebuildStatus.compareAndSet(DOING_REBUILD, REQUIRES_REBUILD);
          throw e;
        }
      }

      ProgressManager.checkCanceled();
      TimeoutUtil.sleep(50);
    }
  }
}
