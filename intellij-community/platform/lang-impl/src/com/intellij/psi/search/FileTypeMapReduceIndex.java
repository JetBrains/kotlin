// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.search;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.indexing.FileBasedIndexExtension;
import com.intellij.util.indexing.FileIndexingState;
import com.intellij.util.indexing.IndexedFile;
import com.intellij.util.indexing.VfsAwareMapReduceIndex;
import com.intellij.util.indexing.impl.IndexStorage;
import com.intellij.util.indexing.impl.MapInputDataDiffBuilder;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Collection;

class FileTypeMapReduceIndex extends VfsAwareMapReduceIndex<FileType, Void> {
  private static final Logger LOG = Logger.getInstance(FileTypeIndexImpl.class);

  FileTypeMapReduceIndex(@NotNull FileBasedIndexExtension<FileType, Void> extension, @NotNull IndexStorage<FileType, Void> storage) throws IOException {
    super(extension, storage);
  }

  @Override
  public @NotNull FileIndexingState getIndexingStateForFile(int fileId, @NotNull IndexedFile file) {
    @NotNull FileIndexingState isIndexed = super.getIndexingStateForFile(fileId, file);
    if (isIndexed != FileIndexingState.UP_TO_DATE) return isIndexed;
    try {
      Collection<FileType> inputData = ((MapInputDataDiffBuilder<FileType, Void>) getKeysDiffBuilder(fileId)).getKeys();
      FileType indexedFileType = ContainerUtil.getFirstItem(inputData);
      return FileTypeKeyDescriptor.INSTANCE.isEqual(indexedFileType, file.getFileType())
             ? FileIndexingState.UP_TO_DATE
             : FileIndexingState.OUT_DATED;
    } catch (IOException e) {
      LOG.error(e);
      return FileIndexingState.OUT_DATED;
    }
  }
}
