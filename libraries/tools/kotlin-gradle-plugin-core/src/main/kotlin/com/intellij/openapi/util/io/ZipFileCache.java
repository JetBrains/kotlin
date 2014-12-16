/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.openapi.util.io;

import com.intellij.openapi.diagnostic.LogUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.ConcurrencyUtil;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipFile;

/**
 * PATCHED VERSION of com.intellij.openapi.util.io.ZipFileCache
 * shutdown() method added to stop background thread.
 */
@SuppressWarnings("UnusedDeclaration")
public class ZipFileCache {
    private static final int PERIOD = 10000;   // disposer schedule, ms
    private static final int TIMEOUT = 30000;  // released file close delay, ms

    private static class CacheRecord {
        private final String path;
        private final ZipFile file;
        private int count = 1;
        private long released = 0;

        private CacheRecord(@NotNull String path, @NotNull ZipFile file) throws IOException {
            this.path = path;
            this.file = file;
        }
    }

    private static final Object ourLock = new Object();
    private static final Map<String, CacheRecord> ourPathCache = ContainerUtil.newTroveMap(FileUtil.PATH_HASHING_STRATEGY);
    private static final Map<ZipFile, CacheRecord> ourFileCache = ContainerUtil.newHashMap();
    private static final Map<ZipFile, Integer> ourQueue = ContainerUtil.newHashMap();

    private static final ScheduledThreadPoolExecutor scheduledCloseExecutor;

    static {
        scheduledCloseExecutor =
                ConcurrencyUtil.newSingleScheduledThreadExecutor("ZipFileCache Dispose", Thread.MIN_PRIORITY);
        scheduledCloseExecutor.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                List<ZipFile> toClose = getFilesToClose(0, System.currentTimeMillis() - TIMEOUT);
                if (toClose != null) {
                    close(toClose);
                }
            }
        }, PERIOD, PERIOD, TimeUnit.MILLISECONDS);
    }

    @NotNull
    public static ZipFile acquire(@NotNull String path) throws IOException {
        path = FileUtil.toCanonicalPath(path);

        synchronized (ourLock) {
            CacheRecord record = ourPathCache.get(path);
            if (record != null) {
                record.count++;
                return record.file;
            }
        }

        CacheRecord record;
        ZipFile file = tryOpen(path);

        synchronized (ourLock) {
            record = ourPathCache.get(path);
            if (record == null) {
                record = new CacheRecord(path, file);
                ourPathCache.put(path, record);
                ourFileCache.put(file, record);
                return file;
            }
            else {
                record.count++;
            }
        }

        close(file);
        return record.file;
    }

    private static ZipFile tryOpen(String path) throws IOException {
        path = FileUtil.toSystemDependentName(path);
        debug("opening %s", path);
        try {
            return new ZipFile(path);
        }
        catch (IOException e) {
            String reason = e.getMessage();
            if ("too many open files".equalsIgnoreCase(reason) && tryCloseFiles() > 0) {
                return new ZipFile(path);
            }
            else {
                throw e;
            }
        }
    }

    private static int tryCloseFiles() {
        List<ZipFile> toClose = getFilesToClose(5, 0);
        if (toClose == null) return 0;
        close(toClose);
        logger().warn("too many open files, closed: " + toClose.size());
        return toClose.size();
    }

    @Nullable
    private static List<ZipFile> getFilesToClose(int limit, long timeout) {
        List<ZipFile> toClose = null;

        synchronized (ourLock) {
            Iterator<CacheRecord> i = ourPathCache.values().iterator();
            while (i.hasNext() && (limit == 0 || toClose == null || toClose.size() < limit)) {
                CacheRecord record = i.next();
                if (record.count <= 0 && (timeout == 0 || record.released <= timeout)) {
                    i.remove();
                    ourFileCache.remove(record.file);
                    if (toClose == null) toClose = ContainerUtil.newArrayList();
                    toClose.add(record.file);
                }
            }
        }

        return toClose;
    }

    public static void release(@NotNull ZipFile file) {
        synchronized (ourLock) {
            CacheRecord record = ourFileCache.get(file);
            if (record != null) {
                record.count--;
                record.released = System.currentTimeMillis();
                logger().assertTrue(record.count >= 0, record.path);
                return;
            }

            Integer count = ourQueue.get(file);
            if (count != null) {
                count--;
                if (count == 0) {
                    ourQueue.remove(file);
                    close(file);
                }
                else {
                    ourQueue.put(file, count);
                }
                return;
            }
        }

        logger().warn(new IllegalArgumentException("stray file: " + file.getName()));
        close(file);
    }

    public static void reset(@NotNull Collection<String> paths) {
        debug("resetting %s", paths);

        List<ZipFile> toClose = ContainerUtil.newSmartList();

        synchronized (ourLock) {
            for (String path : paths) {
                path = FileUtil.toCanonicalPath(path);
                CacheRecord record = ourPathCache.remove(path);
                if (record != null) {
                    ourFileCache.remove(record.file);
                    if (record.count > 0) {
                        ourQueue.put(record.file, record.count);
                    }
                    else {
                        toClose.add(record.file);
                    }
                }
            }
        }

        close(toClose);
    }

    private static void close(@NotNull List<ZipFile> files) {
        for (ZipFile file : files) {
            close(file);
        }
    }

    private static void close(@NotNull ZipFile file) {
        debug("closing %s", file.getName());
        try {
            file.close();
        }
        catch (IOException e) {
            logger().info(file.getName(), e);
        }
    }

    private static Logger logger() {
        return Logger.getInstance(ZipFileCache.class);
    }

    private static void debug(@NotNull String format, Object... args) {
        LogUtil.debug(logger(), format, args);
    }

    public static void shutdown() throws InterruptedException {
        scheduledCloseExecutor.shutdown();

        boolean terminated = scheduledCloseExecutor.awaitTermination(TIMEOUT, TimeUnit.MICROSECONDS);
        if (!terminated) {
            scheduledCloseExecutor.shutdownNow();
        }
    }
}
