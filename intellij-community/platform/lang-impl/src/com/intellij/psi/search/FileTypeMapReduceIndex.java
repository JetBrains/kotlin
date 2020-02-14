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
import java.util.Collection;
import java.util.Map;

class FileTypeMapReduceIndex extends VfsAwareMapReduceIndex<FileType, Void> {
  private static final Logger LOG = Logger.getInstance(FileTypeIndexImpl.class);

  FileTypeMapReduceIndex(@NotNull FileBasedIndexExtension<FileType, Void> extension, @NotNull IndexStorage<FileType, Void> storage) throws IOException {
    super(extension, storage);
  }

  @Override
  public boolean isIndexedStateForFile(int fileId, @NotNull IndexedFile file) {
    boolean isIndexed = super.isIndexedStateForFile(fileId, file);
    if (!isIndexed) return false;
    try {
      Collection<FileType> inputData = ((MapInputDataDiffBuilder<FileType, Void>) getKeysDiffBuilder(fileId)).getKeys();
      FileType indexedFileType = ContainerUtil.getFirstItem(inputData);
      return FileTypeKeyDescriptor.INSTANCE.isEqual(indexedFileType, file.getFileType());
    } catch (IOException e) {
      LOG.error(e);
      return false;
    }
  }
}
