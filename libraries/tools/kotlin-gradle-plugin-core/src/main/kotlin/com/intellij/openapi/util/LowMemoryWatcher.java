/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.openapi.util;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.ConcurrencyUtil;
import com.intellij.util.containers.WeakList;
import org.jetbrains.annotations.NotNull;

import javax.management.ListenerNotFoundException;
import javax.management.Notification;
import javax.management.NotificationEmitter;
import javax.management.NotificationListener;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryNotificationInfo;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * PATCHED VERSION of com.intellij.openapi.util.LowMemoryWatcher
 * shutdown() method added to stop background thread.
 */
@SuppressWarnings("UnusedDeclaration")
public class LowMemoryWatcher {
    private static final long MEM_THRESHOLD = 5 /*MB*/ * 1024 * 1024;

    private static final Logger LOG = Logger.getInstance("#com.intellij.openapi.util.LowMemoryWatcher");

    private static final List<LowMemoryWatcher> ourInstances = new WeakList<LowMemoryWatcher>();
    private static final ThreadPoolExecutor ourExecutor = new ThreadPoolExecutor(0, 1, 10, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(2), ConcurrencyUtil.newNamedThreadFactory("LowMemoryWatcher janitor"));
    private static boolean ourSubmitted;
    private static final Runnable ourJanitor = new Runnable() {
        @Override
        public void run() {
            try {
                for (LowMemoryWatcher watcher : ourInstances) {
                    try {
                        watcher.myRunnable.run();
                    }
                    catch (Throwable e) {
                        LOG.info(e);
                    }
                }
            }
            finally {
                synchronized (ourJanitor) {
                    //noinspection AssignmentToStaticFieldFromInstanceMethod
                    ourSubmitted = false;
                }
            }
        }
    };

    private final Runnable myRunnable;

    private static final NotificationListener lowMemoryListener;

    static {
        for (MemoryPoolMXBean bean : ManagementFactory.getMemoryPoolMXBeans()) {
            if (bean.getType() == MemoryType.HEAP && bean.isUsageThresholdSupported()) {
                long threshold = bean.getUsage().getMax() - MEM_THRESHOLD;
                if (threshold > 0) {
                    bean.setUsageThreshold(threshold);
                    bean.setCollectionUsageThreshold(threshold);
                }
            }
        }

        lowMemoryListener = new NotificationListener() {
            @Override
            public void handleNotification(@NotNull Notification n, Object hb) {
                if (MemoryNotificationInfo.MEMORY_THRESHOLD_EXCEEDED.equals(n.getType()) ||
                    MemoryNotificationInfo.MEMORY_COLLECTION_THRESHOLD_EXCEEDED.equals(n.getType())) {
                    synchronized (ourJanitor) {
                        if (!ourSubmitted) {
                            //noinspection AssignmentToStaticFieldFromInstanceMethod
                            ourSubmitted = true;
                            ourExecutor.submit(ourJanitor);
                        }
                    }
                }
            }
        };

        ((NotificationEmitter)ManagementFactory.getMemoryMXBean()).addNotificationListener(lowMemoryListener, null, null);
    }

    /**
     * Registers a runnable to run on low memory events
     * @return a LowMemoryWatcher instance holding the runnable. This instance should be kept in memory while the
     * low memory notification functionality is needed. As soon as it's garbage-collected, the runnable won't receive any further notifications.
     */
    public static LowMemoryWatcher register(Runnable runnable) {
        return new LowMemoryWatcher(runnable);
    }

    /**
     * Registers a runnable to run on low memory events. The notifications will be issued until parentDisposable is disposed.
     */
    public static void register(Runnable runnable, Disposable parentDisposable) {
        final Ref<LowMemoryWatcher> watcher = Ref.create(new LowMemoryWatcher(runnable));
        Disposer.register(parentDisposable, new Disposable() {
            @Override
            public void dispose() {
                watcher.get().stop();
                watcher.set(null);
            }
        });
    }

    private LowMemoryWatcher(Runnable runnable) {
        myRunnable = runnable;
        ourInstances.add(this);
    }

    public void stop() {
        ourInstances.remove(this);
    }

    public static void shutdown() throws InterruptedException, ListenerNotFoundException {
        ourInstances.clear();
        ((NotificationEmitter)ManagementFactory.getMemoryMXBean()).removeNotificationListener(lowMemoryListener);

        ourExecutor.shutdown();

        boolean terminated = ourExecutor.awaitTermination(1000, TimeUnit.MICROSECONDS);
        if (!terminated) {
            ourExecutor.shutdownNow();
        }
    }
}
