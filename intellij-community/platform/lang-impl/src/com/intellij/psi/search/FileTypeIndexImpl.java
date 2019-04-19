// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.search;

import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeRegistry;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.indexing.*;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.KeyDescriptor;
import org.jetbrains.annotations.NotNull;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;

public final class FileTypeIndexImpl extends ScalarIndexExtension<FileType>
  implements FileBasedIndex.InputFilter, KeyDescriptor<FileType>, DataIndexer<FileType, Void, FileContent> {
  static final ID<FileType, Void> NAME = FileTypeIndex.NAME;

  @NotNull
  @Override
  public ID<FileType, Void> getName() {
    return NAME;
  }

  @NotNull
  @Override
  public DataIndexer<FileType, Void, FileContent> getIndexer() {
    return this;
  }

  @NotNull
  @Override
  public KeyDescriptor<FileType> getKeyDescriptor() {
    return this;
  }

  @NotNull
  @Override
  public FileBasedIndex.InputFilter getInputFilter() {
    return this;
  }

  @Override
  public boolean dependsOnFileContent() {
    return false;
  }

  @Override
  public int getVersion() {
    FileType[] types = FileTypeRegistry.getInstance().getRegisteredFileTypes();
    int version = 2;
    for (FileType type : types) {
      version += type.getName().hashCode();
    }

    version *= 31;
    for (FileTypeRegistry.FileTypeDetector detector : FileTypeRegistry.FileTypeDetector.EP_NAME.getExtensionList()) {
      version += detector.getVersion();
    }
    return version;
  }

  @Override
  public boolean acceptInput(@NotNull VirtualFile file) {
    return !file.isDirectory();
  }

  @Override
  public void save(@NotNull DataOutput out, FileType value) throws IOException {
    EnumeratorStringDescriptor.INSTANCE.save(out, value.getName());
  }

  @Override
  public FileType read(@NotNull DataInput in) throws IOException {
    String read = EnumeratorStringDescriptor.INSTANCE.read(in);
    return FileTypeRegistry.getInstance().findFileTypeByName(read);
  }

  @Override
  public int getHashCode(FileType value) {
    return value.getName().hashCode();
  }

  @Override
  public boolean isEqual(FileType val1, FileType val2) {
    if (val1 instanceof SubstitutedFileType) val1 = ((SubstitutedFileType)val1).getOriginalFileType();
    if (val2 instanceof SubstitutedFileType) val2 = ((SubstitutedFileType)val2).getOriginalFileType();
    return Comparing.equal(val1, val2);
  }

  @NotNull
  @Override
  public Map<FileType, Void> map(@NotNull FileContent inputData) {
    return Collections.singletonMap(inputData.getFileType(), null);
  }
}
