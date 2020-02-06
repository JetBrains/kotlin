// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.hash;

import com.intellij.openapi.project.Project;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.hash.ContentHashEnumerator;
import com.intellij.util.indexing.FileBasedIndexExtension;
import com.intellij.util.indexing.FileContent;
import com.intellij.util.indexing.ID;
import com.intellij.util.indexing.UpdatableIndex;
import com.intellij.util.indexing.provided.SharedIndexExtension;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

@ApiStatus.Internal
public class SharedIndexChunk<K, V> {
  private final Path myChunkRoot;
  private final ID<?, ?> myIndexName;
  private final int myChunkId;
  private final UpdatableIndex<K, V, FileContent> myIndex;
  private final long myCreationTimestamp;
  private final Set<Project> myProjects = ContainerUtil.newConcurrentSet();

  @ApiStatus.Internal
  public SharedIndexChunk(@NotNull Path chunkRoot,
                          @NotNull ID<K, V> indexName,
                          int chunkId,
                          long timestamp,
                          boolean empty,
                          @NotNull SharedIndexExtension<K, V> sharedExtension,
                          @NotNull FileBasedIndexExtension<K, V> originalExtension,
                          @NotNull FileContentHashIndex hashIndex) throws IOException {
    myChunkRoot = chunkRoot;
    myIndexName = indexName;
    myChunkId = chunkId;
    myCreationTimestamp = timestamp;

    myIndex = empty ? EmptyIndex.getInstance() : new HashBasedMapReduceIndex<>(this, sharedExtension, originalExtension, hashIndex);
  }

  void close() {
    myIndex.dispose();
    SharedIndexChunkConfiguration.getInstance().disposeIndexChunkData(myIndexName, myChunkId);
  }

  public @NotNull ID<?, ?> getIndexName() {
    return myIndexName;
  }

  public @NotNull Path getPath() {
    return myChunkRoot.resolve(myIndexName.getName());
  }

  public int getChunkId() {
    return myChunkId;
  }

  public void addProject(@NotNull Project project) {
    myProjects.add(project);
  }

  public boolean removeProject(@NotNull Project project) {
    myProjects.remove(project);
    return myProjects.isEmpty();
  }

  @NotNull
  public UpdatableIndex<K, V, FileContent> getIndex() {
    return myIndex;
  }
}
