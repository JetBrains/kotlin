/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package org.jetbrains.jps.service.impl;

import com.intellij.util.ConcurrencyUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.service.SharedThreadPool;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * @author nik
 */
public class SharedThreadPoolImpl extends SharedThreadPool {
  private final ExecutorService myService = Executors.newCachedThreadPool(ConcurrencyUtil.newNamedThreadFactory("JPS thread pool", true, Thread.NORM_PRIORITY));

  @Override
  public void execute(@NotNull Runnable command) {
    executeOnPooledThread(command);
  }

  @NotNull
  @Override
  public Future<?> executeOnPooledThread(@NotNull final Runnable action) {
    return myService.submit(() -> {
      try {
        action.run();
      }
      finally {
        Thread.interrupted(); // reset interrupted status
      }
    });
  }
}
