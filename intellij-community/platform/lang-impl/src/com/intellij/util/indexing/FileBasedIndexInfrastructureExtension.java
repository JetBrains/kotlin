// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.stubs.StubIndexKey;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

@ApiStatus.Internal
public interface FileBasedIndexInfrastructureExtension {
  ExtensionPointName<FileBasedIndexInfrastructureExtension> EP_NAME = ExtensionPointName.create("com.intellij.fileBasedIndexInfrastructureExtension");

  /**
   * This notification is sent from the IDE to let the extension point implementation
   * update it's internal state in order to supply indexes.
   * Extension point must not run any heavy tasks in this thread.
   * @param indexingIndicator used only to track cancellation of the indexing, must not be used for updating texts/fractions.
   */
  void processIndexingProject(@NotNull Project project, @NotNull ProgressIndicator indexingIndicator);

  /**
   * This notification is sent when the indexing is started and there are no files detected to index
   */
  void noFilesFoundToProcessIndexingProject(@NotNull Project project, @NotNull ProgressIndicator indexingIndicator);

  interface FileIndexingStatusProcessor {
    /**
     * Processes up to date file while "scanning files to index" in progress.
     */
    void processUpToDateFile(@NotNull VirtualFile file, int inputId, @NotNull ID<?, ?> indexId);

    /**
     * Whether the given file has index provided by this extension.
     */
    @ApiStatus.Experimental
    boolean hasIndexForFile(@NotNull VirtualFile file, int inputId, @NotNull FileBasedIndexExtension<?, ?> extension);
  }

  @Nullable
  FileIndexingStatusProcessor createFileIndexingStatusProcessor(@NotNull Project project);


  /**
   * Allows the extension point to replace the original {@link UpdatableIndex} for
   * the given {@param indexExtension} with a combined index (base part from {@link FileBasedIndexImpl} and customizable one)
   * that that uses the internal state of the extension to supply indexes
   * @return wrapper or null.
   */
  @Nullable
  <K, V> UpdatableIndex<K, V, FileContent> combineIndex(@NotNull FileBasedIndexExtension<K, V> indexExtension,
                                                        @NotNull UpdatableIndex<K, V, FileContent> baseIndex) throws IOException;


  /**
   * Notifies the extension to handle that version of existing file based index version has been changed.
   *
   * Actually {@link FileBasedIndex} notifies even if index composite version (extension version + implementation version)
   * is changed {@link FileBasedIndexImpl#getIndexExtensionVersion(FileBasedIndexExtension)}.
   *
   * @param indexId that version is updated.
   */
  void onFileBasedIndexVersionChanged(@NotNull ID<?, ?> indexId);

  /**
   * Notifies the extension to handle that version of existing stub index has been changed.
   *
   * @param indexId that version is updated.
   */
  void onStubIndexVersionChanged(@NotNull StubIndexKey<?, ?> indexId);

  /**
   * Executed when IntelliJ is open it's indexes (IDE start or plugin load/unload).
   * All necessarily needed connections and resources should be open here.
   *
   * This method and {@link FileBasedIndexInfrastructureExtension#shutdown()} synchronize
   * lifecycle of an extension with {@link FileBasedIndexImpl}.
   **/
  @NotNull
  InitializationResult initialize();

  /**
   * Executed when IntelliJ is shutting down it's indexes (IDE shutdown or plugin load/unload). It is the best time
   * for the component to flush it's state to the disk and close all pending connections.
   *
   * This method and {@link FileBasedIndexInfrastructureExtension#initialize()} synchronize
   * lifecycle of an extension with {@link FileBasedIndexImpl}.
   */
  void shutdown();

  /**
   * When index infrastructure extension change it's version (for example data format has been changed)
   * all indexed data should be invalidate and full index rebuild will be requested
   */
  int getVersion();

  enum InitializationResult {
    SUCCESSFULLY, INDEX_REBUILD_REQUIRED
  }
}
