// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.zipFs;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;

public final class UncompressedZipEntryFileAttributes implements BasicFileAttributes {
  private final UncompressedZipFileSystem.ZipTreeNode myNode;

  public UncompressedZipEntryFileAttributes(@NotNull UncompressedZipPath path) throws IOException {
    myNode = UncompressedZipFileSystemProvider.find(path);
  }

  @Override
  public FileTime lastModifiedTime() {
    throw new UnsupportedOperationException();
  }

  @Override
  public FileTime lastAccessTime() {
    throw new UnsupportedOperationException();
  }

  @Override
  public FileTime creationTime() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isRegularFile() {
    return !isDirectory();
  }

  @Override
  public boolean isDirectory() {
    return myNode.isDirectory();
  }

  @Override
  public boolean isSymbolicLink() {
    return false;
  }

  @Override
  public boolean isOther() {
    return false;
  }

  @Override
  public long size() {
    return myNode.getEntry().getCompressedSize();
  }

  @Override
  public Object fileKey() {
    return null;
  }
}
