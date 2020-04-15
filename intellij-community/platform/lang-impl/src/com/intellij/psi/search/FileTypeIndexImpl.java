// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.search;

import com.intellij.openapi.fileTypes.FileType;
import com.intellij.util.indexing.*;
import com.intellij.util.indexing.impl.IndexStorage;
import com.intellij.util.io.KeyDescriptor;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Collections;

public final class FileTypeIndexImpl
        extends ScalarIndexExtension<FileType>
        implements CustomImplementationFileBasedIndexExtension<FileType, Void> {
  @NotNull
  @Override
  public ID<FileType, Void> getName() {
    return FileTypeIndex.NAME;
  }

  @NotNull
  @Override
  public DataIndexer<FileType, Void, FileContent> getIndexer() {
    return in -> Collections.singletonMap(in.getFileType(), null);
  }

  @NotNull
  @Override
  public KeyDescriptor<FileType> getKeyDescriptor() {
    return new FileTypeKeyDescriptor();
  }

  @NotNull
  @Override
  public FileBasedIndex.InputFilter getInputFilter() {
    return file -> !file.isDirectory();
  }

  @Override
  public boolean dependsOnFileContent() {
    return false;
  }

  @Override
  public int getVersion() {
    return 3;
  }

  @NotNull
  @Override
  public UpdatableIndex<FileType, Void, FileContent> createIndexImplementation(@NotNull FileBasedIndexExtension<FileType, Void> extension, @NotNull IndexStorage<FileType, Void> storage) throws StorageException, IOException {
    return new FileTypeMapReduceIndex(extension, storage);
  }
}
