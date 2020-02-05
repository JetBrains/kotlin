// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.provided;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.util.Consumer;
import com.intellij.util.indexing.IndexInfrastructureVersion;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Set;

/**
 * This extension point is used to supply prebuild indexes to the IDE
 */
public interface SharedIndexChunkLocator {
  ExtensionPointName<SharedIndexChunkLocator> EP_NAME = ExtensionPointName.create("com.intellij.sharedIndexChunkLocator");

  /**
   * Executed every time an indexing attempt is performed by an IDE.
   * This method is blocking. Use {@param indicator} to report the progress and check
   * for cancellation via {@link ProgressIndicator#checkCanceled()}.
   * Use {@param descriptorProcessor} to submit detected matching indexes chunks.
   * This method should work fast and it should only download/process indexes metadata.
   * The actual download run with {@link ChunkDescriptor#downloadChunk(Path, ProgressIndicator)} method later.
   */
  void locateIndex(@NotNull Project project,
                   @NotNull Collection<? extends OrderEntry> entries,
                   @NotNull Consumer<? super ChunkDescriptor> descriptorProcessor,
                   @NotNull ProgressIndicator indicator);

  /**
   * A handler for a possible indexes chunk that this extension is able to supply
   */
  interface ChunkDescriptor {
    /**
     * An application wide unique identifier of that indexes portion.
     * That key is used by the IDE to avoid re-downloading same indexes one more times
     */
    @NotNull
    String getChunkUniqueId();

    /**
     * Version of index infrastructure for which shared index chunk was built.
     * <p>
     * It contains main infrastructure versions (e.g. versions of persistent data structures)
     * as well as index extension versions.
     */
    @NotNull
    IndexInfrastructureVersion getSupportedInfrastructureVersion();

    /**
     * Matching order entries from the {@link #locateIndex(Project, Set, Consumer, ProgressIndicator)} call
     */
    @NotNull
    Collection<? extends OrderEntry> getOrderEntries();

    /**
     * Materialize the index chunk
     */
    void downloadChunk(@NotNull Path targetFile,
                       @NotNull ProgressIndicator indicator) throws IOException;
  }
}
