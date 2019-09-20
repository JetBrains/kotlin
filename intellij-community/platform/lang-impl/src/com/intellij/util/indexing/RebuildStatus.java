// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing;

import com.intellij.openapi.progress.ProgressManager;
import com.intellij.util.ThrowableRunnable;
import com.intellij.util.TimeoutUtil;
import gnu.trove.THashMap;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author peter
 */
enum RebuildStatus {
  OK,
  REQUIRES_REBUILD,
  DOING_REBUILD;

  private static final Map<ID<?, ?>, AtomicReference<RebuildStatus>> ourRebuildStatus = new THashMap<>();

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

  static void clearIndexIfNecessary(ID<?, ?> indexId, ThrowableRunnable<? extends StorageException> clearAction) throws StorageException {
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
