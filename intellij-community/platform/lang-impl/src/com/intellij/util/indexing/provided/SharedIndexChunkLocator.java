// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.provided;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.OrderEntry;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.util.Set;

public interface SharedIndexChunkLocator {
  ExtensionPointName<SharedIndexChunkLocator> EP_NAME = ExtensionPointName.create("com.intellij.sharedIndexChunkLocator");

  void locateIndex(@NotNull Project project,
                   @NotNull Set<OrderEntry> entries,
                   @NotNull ProgressIndicator indicator,
                   @NotNull SharedIndexHandler handler);

  interface SharedIndexHandler {
    boolean onIndexAvailable(@NotNull ChunkDescriptor descriptor);

    void onIndexReceived(@NotNull ChunkDescriptor descriptor, @NotNull InputStream inputStream);
  }

  class ChunkDescriptor {
    @NotNull
    private final String myHash;

    public ChunkDescriptor(@NotNull String hash) {myHash = hash;}

    @NotNull
    public String getHash() {
      return myHash;
    }
  }
}
