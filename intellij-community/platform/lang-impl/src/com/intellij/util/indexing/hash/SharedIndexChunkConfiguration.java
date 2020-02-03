// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.hash;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.util.Processor;
import com.intellij.util.hash.ContentHashEnumerator;
import com.intellij.util.indexing.ID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Set;

public interface SharedIndexChunkConfiguration {

  @NotNull
  static SharedIndexChunkConfiguration getInstance() {
    return ServiceManager.getService(SharedIndexChunkConfiguration.class);
  }

  @Nullable
  <Value, Key> HashBasedMapReduceIndex<Key, Value> getChunk(@NotNull ID<Key, Value> indexId, int chunkId);

  <Value, Key> void processChunks(@NotNull ID<Key, Value> indexId, @NotNull Processor<HashBasedMapReduceIndex<Key, Value>> processor);

  long tryEnumerateContentHash(byte[] hash) throws IOException;

  void locateIndexes(@NotNull Project project,
                     @NotNull Set<OrderEntry> entry,
                     @NotNull ProgressIndicator indicator);

  void closeEnumerator(ContentHashEnumerator enumerator, int chunkId);
}
