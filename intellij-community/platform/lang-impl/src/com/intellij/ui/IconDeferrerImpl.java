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

/*
 * @author max
 */
package com.intellij.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.impl.ProjectLifecycleListener;
import com.intellij.openapi.util.LowMemoryWatcher;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.util.Function;
import com.intellij.util.containers.FixedHashMap;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.messages.MessageBusConnection;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.List;
import java.util.Map;

public class IconDeferrerImpl extends IconDeferrer {
  private final Object LOCK = new Object();
  private final Map<Object, Icon> myIconsCache = new FixedHashMap<>(Registry.intValue("ide.icons.deferrerCacheSize"));
  private long myLastClearTimestamp;

  public IconDeferrerImpl(@NotNull MessageBus bus) {
    final MessageBusConnection connection = bus.connect();
    connection.subscribe(PsiModificationTracker.TOPIC, this::clear);
    // update "locked" icon
    connection.subscribe(VirtualFileManager.VFS_CHANGES, new BulkFileListener() {
      @Override
      public void after(@NotNull List<? extends VFileEvent> events) {
        clear();
      }
    });
    connection.subscribe(ProjectLifecycleListener.TOPIC, new ProjectLifecycleListener() {
      @Override
      public void afterProjectClosed(@NotNull Project project) {
        clear();
      }
    });
    LowMemoryWatcher.register(this::clear, connection);
  }

  protected final void clear() {
    synchronized (LOCK) {
      myIconsCache.clear();
      myLastClearTimestamp++;
    }
  }

  @Override
  public <T> Icon defer(final Icon base, final T param, @NotNull final Function<? super T, ? extends Icon> evaluator) {
    return deferImpl(base, param, evaluator, false);
  }

  @Override
  public <T> Icon deferAutoUpdatable(Icon base, T param, @NotNull Function<? super T, ? extends Icon> evaluator) {
    return deferImpl(base, param, evaluator, true);
  }

  private <T> Icon deferImpl(Icon base, T param, @NotNull Function<? super T, ? extends Icon> evaluator, final boolean autoUpdatable) {
    if (myEvaluationIsInProgress.get().booleanValue()) {
      return evaluator.fun(param);
    }

    synchronized (LOCK) {
      Icon cached = myIconsCache.get(param);
      if (cached != null) {
        return cached;
      }
      final long started = myLastClearTimestamp;
      Icon result = new DeferredIconImpl<>(base, param, evaluator, (DeferredIconImpl<T> source, T key, Icon r) -> {
        synchronized (LOCK) {
          // check if our results is not outdated yet
          if (started == myLastClearTimestamp) {
            myIconsCache.put(key, autoUpdatable ? source : r);
          }
        }
      }, autoUpdatable);
      myIconsCache.put(param, result);

      return result;
    }
  }

  private static final ThreadLocal<Boolean> myEvaluationIsInProgress = ThreadLocal.withInitial(() -> Boolean.FALSE);

  static void evaluateDeferred(@NotNull Runnable runnable) {
    try {
      myEvaluationIsInProgress.set(Boolean.TRUE);
      runnable.run();
    }
    finally {
      myEvaluationIsInProgress.set(Boolean.FALSE);
    }
  }

  @Override
  public boolean equalIcons(Icon icon1, Icon icon2) {
    return DeferredIconImpl.equalIcons(icon1, icon2);
  }
}
