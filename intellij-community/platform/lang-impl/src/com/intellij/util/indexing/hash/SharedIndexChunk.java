// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.hash;

import com.intellij.openapi.project.Project;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.hash.ContentHashEnumerator;
import com.intellij.util.indexing.FileBasedIndexExtension;
import com.intellij.util.indexing.ID;
import com.intellij.util.indexing.provided.SharedIndexExtension;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

@ApiStatus.Internal
public class SharedIndexChunk {
  private final Path myChunkRoot;
  private final ID<?, ?> myIndexName;
  private final int myChunkId;
  @NotNull
  private final ContentHashEnumerator myEnumerator;
  private volatile HashBasedMapReduceIndex myIndex;
  private final long myCreationTimestamp;
  private final Set<Project> myProjects = ContainerUtil.newConcurrentSet();

  @ApiStatus.Internal
  public SharedIndexChunk(@NotNull Path chunkRoot,
                          @NotNull ID<?, ?> indexName,
                          int chunkId,
                          @NotNull ContentHashEnumerator enumerator,
                          long timestamp) {
    myChunkRoot = chunkRoot;
    myIndexName = indexName;
    myChunkId = chunkId;
    myEnumerator = enumerator;
    myCreationTimestamp = timestamp;
  }

  @ApiStatus.Internal
  @NotNull
  public <Key, Value> HashBasedMapReduceIndex<Key, Value> open(@NotNull SharedIndexExtension<Key, Value> sharedExtension,
                                                               @NotNull FileBasedIndexExtension<Key, Value> originalExtension,
                                                               @NotNull FileContentHashIndex hashIndex) throws IOException {
    assert myIndex == null;
    myIndex = new HashBasedMapReduceIndex<>(this, sharedExtension, originalExtension, hashIndex);
    return myIndex;
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

  public <Key, Value> HashBasedMapReduceIndex<Key, Value> getIndex() {
    return (HashBasedMapReduceIndex<Key, Value>)myIndex;
  }
}
