// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.search;

import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeRegistry;
import com.intellij.util.indexing.*;
import com.intellij.util.indexing.impl.IndexStorage;
import com.intellij.util.io.KeyDescriptor;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Collections;

public final class FileTypeIndexImpl
        extends ScalarIndexExtension<FileType>
        implements CustomImplementationFileBasedIndexExtension<FileType, Void> {
  static final ID<FileType, Void> NAME = FileTypeIndex.NAME;

  @NotNull
  @Override
  public ID<FileType, Void> getName() {
    return NAME;
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
    int version = 2;

    if (!InvertedIndex.ARE_COMPOSITE_INDEXERS_ENABLED) {
      FileType[] types = FileTypeRegistry.getInstance().getRegisteredFileTypes();
      for (FileType type : types) {
        version += type.getName().hashCode();
      }

      version *= 31;
      for (FileTypeRegistry.FileTypeDetector detector : FileTypeRegistry.FileTypeDetector.EP_NAME.getExtensionList()) {
        version += detector.getVersion();
      }
    }

    return version;
  }

  @NotNull
  @Override
  public UpdatableIndex<FileType, Void, FileContent> createIndexImplementation(@NotNull FileBasedIndexExtension<FileType, Void> extension, @NotNull IndexStorage<FileType, Void> storage) throws StorageException, IOException {
    return new FileTypeMapReduceIndex(extension, storage);
  }
}
