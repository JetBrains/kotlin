// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.zipFs;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileStoreAttributeView;

class UncompressedZipFileStore extends FileStore {
  @NotNull
  private final UncompressedZipFileSystem mySystem;

  UncompressedZipFileStore(@NotNull UncompressedZipFileSystem system) {
    mySystem = system;
  }

  @Override
  public String name() {
    return "zip0";
  }

  @Override
  public String type() {
    return "zip0";
  }

  @Override
  public boolean isReadOnly() {
    return true;
  }

  @Override
  public long getTotalSpace() throws IOException {
    return mySystem.getChannel().size();
  }

  @Override
  public long getUsableSpace() throws IOException {
    return mySystem.getChannel().size();
  }

  @Override
  public long getUnallocatedSpace() {
    return 0;
  }

  @Override
  public boolean supportsFileAttributeView(Class<? extends FileAttributeView> type) {
    return false;
  }

  @Override
  public boolean supportsFileAttributeView(String name) {
    return false;
  }

  @Override
  public <V extends FileStoreAttributeView> V getFileStoreAttributeView(Class<V> type) {
    return null;
  }

  @Override
  public Object getAttribute(String attribute) {
    throw new UnsupportedOperationException();
  }
}
