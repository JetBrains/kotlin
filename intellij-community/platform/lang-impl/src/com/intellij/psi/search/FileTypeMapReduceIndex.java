// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.search;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.util.Comparing;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.indexing.*;
import com.intellij.util.indexing.impl.IndexStorage;
import com.intellij.util.indexing.impl.MapInputDataDiffBuilder;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Map;

class FileTypeMapReduceIndex extends VfsAwareMapReduceIndex<FileType, Void, FileContent> {
  private static final Logger LOG = Logger.getInstance(FileTypeIndexImpl.class);

  FileTypeMapReduceIndex(@NotNull FileBasedIndexExtension<FileType, Void> extension, @NotNull IndexStorage<FileType, Void> storage) throws IOException {
    super(extension, storage);
  }

  @Override
  public boolean isIndexedStateForFile(int fileId, @NotNull IndexedFile file) {
    boolean isIndexed = super.isIndexedStateForFile(fileId, file);
    if (!InvertedIndex.ARE_COMPOSITE_INDEXERS_ENABLED) return isIndexed;
    if (isIndexed) {
      try {
        Map<FileType, Void> inputData = ((MapInputDataDiffBuilder<FileType, Void>) getKeysDiffBuilder(fileId)). getMap();
        FileType indexedFileType = ContainerUtil.getFirstItem(inputData.keySet());
        // can be null if file type name is outdated
        return Comparing.equal(indexedFileType, file.getFileType());
      } catch (IOException e) {
        LOG.error(e);
      }
    }
    return isIndexed;
  }
}
