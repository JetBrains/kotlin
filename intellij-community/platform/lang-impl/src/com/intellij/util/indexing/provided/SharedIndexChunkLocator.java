// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.provided;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.util.Consumer;
import com.intellij.util.Processor;
import com.intellij.util.ThrowableConsumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.concurrency.Promise;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Set;

public interface SharedIndexChunkLocator {
  ExtensionPointName<SharedIndexChunkLocator> EP_NAME = ExtensionPointName.create("com.intellij.sharedIndexChunkLocator");

  void locateIndex(@NotNull Project project,
                   @NotNull Set<OrderEntry> entries,
                   @NotNull Processor<ChunkDescriptor> descriptorProcessor,
                   @NotNull ProgressIndicator indicator);

  interface ChunkDescriptor {
    @NotNull
    String getChunkRootName();

    @NotNull
    Set<OrderEntry> getTargetOrderEntries();

    void download(@NotNull ThrowableConsumer<? super InputStream, ? extends IOException> callback, @NotNull ProgressIndicator indicator) throws IOException;
  }
}
