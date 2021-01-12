// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.search;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.indexing.*;
import com.intellij.util.indexing.impl.IndexStorage;
import com.intellij.util.indexing.impl.MapInputDataDiffBuilder;
import com.intellij.util.indexing.impl.storage.VfsAwareMapReduceIndex;
import com.intellij.util.io.IOUtil;
import com.intellij.util.io.PersistentStringEnumerator;
import com.intellij.util.io.StorageLockContext;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;

class FileTypeMapReduceIndex extends VfsAwareMapReduceIndex<FileType, Void> {
  private static final Logger LOG = Logger.getInstance(FileTypeIndexImpl.class);
  private PersistentStringEnumerator myFileTypeNameEnumerator;

  FileTypeMapReduceIndex(@NotNull FileBasedIndexExtension<FileType, Void> extension, @NotNull IndexStorage<FileType, Void> storage) throws IOException {
    super(extension, storage);
    myFileTypeNameEnumerator = createFileTypeNameEnumerator();
  }

  @Override
  public @NotNull FileIndexingState getIndexingStateForFile(int fileId, @NotNull IndexedFile file) {
    @NotNull FileIndexingState isIndexed = super.getIndexingStateForFile(fileId, file);
    if (isIndexed != FileIndexingState.UP_TO_DATE) return isIndexed;
    try {
      Collection<FileType> inputData = ((MapInputDataDiffBuilder<FileType, Void>) getKeysDiffBuilder(fileId)).getKeys();
      FileType indexedFileType = ContainerUtil.getFirstItem(inputData);
      return getExtension().getKeyDescriptor().isEqual(indexedFileType, file.getFileType())
             ? FileIndexingState.UP_TO_DATE
             : FileIndexingState.OUT_DATED;
    } catch (IOException e) {
      LOG.error(e);
      return FileIndexingState.OUT_DATED;
    }
  }

  @Override
  protected void doFlush() throws IOException, StorageException {
    super.doFlush();
    myFileTypeNameEnumerator.force();
  }

  @Override
  protected void doDispose() throws StorageException {
    try {
      super.doDispose();
    } finally {
      IOUtil.closeSafe(LOG, myFileTypeNameEnumerator);
    }
  }

  @Override
  protected void doClear() throws StorageException, IOException {
    super.doClear();
    IOUtil.closeSafe(LOG, myFileTypeNameEnumerator);
    IOUtil.deleteAllFilesStartingWith(getFileTypeNameEnumeratorPath().toFile());
    myFileTypeNameEnumerator = createFileTypeNameEnumerator();
  }

  public int getFileTypeId(String name) throws IOException {
    return myFileTypeNameEnumerator.enumerate(name);
  }

  public String getFileTypeName(int id) throws IOException {
    return myFileTypeNameEnumerator.valueOf(id);
  }

  @NotNull
  private static PersistentStringEnumerator createFileTypeNameEnumerator() throws IOException {
    return new PersistentStringEnumerator(getFileTypeNameEnumeratorPath(),  128, true, new StorageLockContext(true));
  }

  @NotNull
  private static Path getFileTypeNameEnumeratorPath() {
    return IndexInfrastructure.getIndexRootDir(FileTypeIndex.NAME).toPath().resolve("file.type.names");
  }
}
