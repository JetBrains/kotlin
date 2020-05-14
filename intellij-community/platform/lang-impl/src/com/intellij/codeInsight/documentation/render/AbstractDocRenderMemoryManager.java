// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.documentation.render;

import com.intellij.diagnostic.VMOptions;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.registry.Registry;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;

abstract class AbstractDocRenderMemoryManager<T> {
  private static final Logger LOG = Logger.getInstance(AbstractDocRenderMemoryManager.class);

  private final LinkedHashMap<T, Integer> myNotPaintedCache = new LinkedHashMap<>();
  private final LinkedHashMap<T, Integer> myPaintedCache = new LinkedHashMap<>();
  private final int myCacheSizeLimitKb;
  private int myTotalSize;

  AbstractDocRenderMemoryManager(@NotNull String sizeRegistryKey) {
    int memorySizeMb = 750; // default value, if something goes wrong
    try {
      memorySizeMb = VMOptions.readOption(VMOptions.MemoryKind.HEAP, true);
    }
    catch (Throwable e) {
      LOG.error("Failed to get Xmx", e);
    }
    int cacheSize = 10_000; // minimum value
    try {
      cacheSize = Math.max(cacheSize, (int) (memorySizeMb * 1024 * Registry.get(sizeRegistryKey).asDouble()));
    }
    catch (Throwable e) {
      LOG.error("Error calculating cache size limit", e);
    }
    myCacheSizeLimitKb = cacheSize;
    LOG.debug("Cache size: " + myCacheSizeLimitKb + "kB");
  }

  synchronized void register(@NotNull T object, int sizeKb) {
    unregister(object);
    myNotPaintedCache.put(object, sizeKb);
    myTotalSize += sizeKb;

    // trim the cache
    while (myNotPaintedCache.size() > 1 /* don't remove just registered object */ && myTotalSize > myCacheSizeLimitKb) {
      T toRemove = myNotPaintedCache.keySet().iterator().next();
      destroy(toRemove);
      assert !myNotPaintedCache.containsKey(toRemove); // 'destroy' is supposed to call 'unregister'
    }
    while (!myPaintedCache.isEmpty() && myTotalSize > myCacheSizeLimitKb) {
      T toRemove = myPaintedCache.keySet().iterator().next();
      destroy(toRemove);
      assert !myPaintedCache.containsKey(toRemove); // 'destroy' is supposed to call 'unregister'
    }
  }

  synchronized void unregister(@NotNull T object) {
    Integer oldSize = myNotPaintedCache.remove(object);
    if (oldSize != null) myTotalSize -= oldSize;
    oldSize = myPaintedCache.remove(object);
    if (oldSize != null) myTotalSize -= oldSize;
  }

  synchronized void notifyPainted(@NotNull T object) {
    Integer size = myNotPaintedCache.remove(object);
    if (size == null) {
      size = myPaintedCache.remove(object);
    }
    if (size != null) {
      myPaintedCache.put(object, size);
    }
  }

  abstract void destroy(@NotNull T object);
}
