// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.largeFilesEditor.file;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileSystem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

class MockVirtualFile extends VirtualFile {

  private final File myFile;

  protected MockVirtualFile(File file) {
    myFile = file;
  }

  @NotNull
  @Override
  public String getUrl() {
    return myFile.getPath();
  }

  @NotNull
  @Override
  public String getName() {
    throw new UnsupportedOperationException();
  }

  @NotNull
  @Override
  public VirtualFileSystem getFileSystem() {
    throw new UnsupportedOperationException();
  }

  @NotNull
  @Override
  public String getPath() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isWritable() {
    return true;
  }

  @Override
  public boolean isDirectory() {
    return false;
  }

  @Override
  public boolean isValid() {
    return true;
  }

  @Override
  public VirtualFile getParent() {
    return null;
  }

  @Override
  public VirtualFile[] getChildren() {
    return VirtualFile.EMPTY_ARRAY;
  }

  @NotNull
  @Override
  public OutputStream getOutputStream(Object requestor, long newModificationStamp, long newTimeStamp) {
    throw new UnsupportedOperationException();
  }

  @NotNull
  @Override
  public byte[] contentsToByteArray() {
    return new byte[0];
  }

  @Override
  public long getTimeStamp() {
    return 0;
  }

  @Override
  public long getLength() {
    return 0;
  }

  @Override
  public void refresh(boolean asynchronous, boolean recursive, @Nullable Runnable postRunnable) {

  }

  @Override
  public InputStream getInputStream() {
    return null;
  }
}
