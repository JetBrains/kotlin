// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.documentation.render;

import com.intellij.diagnostic.VMOptions;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.registry.Registry;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;

class DocRendererMemoryManager {
  private static final Logger LOG = Logger.getInstance(DocRendererMemoryManager.class);

  private static final int CACHE_SIZE;
  static {
    int memorySizeMb = 750; // default value, if something goes wrong
    try {
      memorySizeMb = VMOptions.readOption(VMOptions.MemoryKind.HEAP, true);
    }
    catch (Throwable e) {
      LOG.error("Failed to get Xmx", e);
    }
    int cacheSize = 200; // default value, if something goes wrong
    try {
      cacheSize = Math.max(20, (int) (Registry.get("doc.render.cache.size.per.mb").asDouble() * memorySizeMb));
    }
    catch (Throwable e) {
      LOG.error("Error calculating cache size", e);
    }
    CACHE_SIZE = cacheSize;
    LOG.debug("Cache size: " + CACHE_SIZE);
  }

  @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
  private static final LinkedHashMap<DocRenderer, Void> CACHE = new LinkedHashMap<DocRenderer, Void>(CACHE_SIZE, 0.75f, true) {
    @Override
    protected boolean removeEldestEntry(Map.Entry<DocRenderer, Void> eldest) {
      if (size() > CACHE_SIZE) {
        LOG.trace("Clearing cached rendered view");
        eldest.getKey().clearCachedComponent();
        return true;
      }
      return false;
    }
  };

  static void onRendererComponentUsage(@NotNull DocRenderer renderer) {
    CACHE.put(renderer, null);
  }

  static void stopTracking(@NotNull DocRenderer renderer) {
    CACHE.remove(renderer);
  }
}
