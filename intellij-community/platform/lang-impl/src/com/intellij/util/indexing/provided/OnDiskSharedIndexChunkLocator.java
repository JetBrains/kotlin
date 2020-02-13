// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.provided;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.util.indexing.IndexInfrastructureVersion;
import com.intellij.util.io.PathKt;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class OnDiskSharedIndexChunkLocator implements SharedIndexChunkLocator {
  public static final String ROOT_PROP = "on.disk.shared.index.root"; //NON-NLS

  @Override
  public List<ChunkDescriptor> locateIndex(@NotNull Project project,
                                           @NotNull Collection<? extends OrderEntry> entries,
                                           @NotNull ProgressIndicator indicator) {
    String indexRoot = System.getProperty(ROOT_PROP);
    if (indexRoot == null) return Collections.emptyList();
    File indexZip = new File(indexRoot);
    if (!indexZip.exists() || !indexZip.isFile()) return Collections.emptyList();
    String name = indexZip.getName();
    return Collections.singletonList(new ChunkDescriptor() {
      @Override
      public @NotNull String getChunkUniqueId() {
        return name;
      }

      @Override
      public @NotNull IndexInfrastructureVersion getSupportedInfrastructureVersion() {
        return IndexInfrastructureVersion.getIdeVersion();
      }

      @Override
      public @NotNull Collection<? extends OrderEntry> getOrderEntries() {
        return entries;
      }

      @Override
      public void downloadChunk(@NotNull Path targetFile, @NotNull ProgressIndicator indicator) {
        PathKt.copy(indexZip.toPath(), targetFile);
      }
    });
  }
}
