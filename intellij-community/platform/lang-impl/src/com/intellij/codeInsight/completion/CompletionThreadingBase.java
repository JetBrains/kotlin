/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package com.intellij.codeInsight.completion;

import com.intellij.openapi.progress.ProgressManager;

import java.util.Objects;

public abstract class CompletionThreadingBase implements CompletionThreading {
  protected final static ThreadLocal<Boolean> ourIsInBatchUpdate = ThreadLocal.withInitial(() -> Boolean.FALSE);

  public static void withBatchUpdate(Runnable runnable, CompletionProcess process) {
    if (ourIsInBatchUpdate.get().booleanValue() || !(process instanceof CompletionProgressIndicator)) {
      runnable.run();
      return;
    }

    try {
      ourIsInBatchUpdate.set(Boolean.TRUE);
      runnable.run();
      ProgressManager.checkCanceled();
      CompletionProgressIndicator currentIndicator = (CompletionProgressIndicator)process;
      CompletionThreadingBase threading = Objects.requireNonNull(currentIndicator.getCompletionThreading());
      threading.flushBatchResult(currentIndicator);
    } finally {
      ourIsInBatchUpdate.set(Boolean.FALSE);
    }
  }

  protected abstract void flushBatchResult(CompletionProgressIndicator indicator);
}
