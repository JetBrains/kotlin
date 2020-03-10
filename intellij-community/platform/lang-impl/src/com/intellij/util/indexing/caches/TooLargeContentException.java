// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.caches;

import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

public final class TooLargeContentException extends Exception {
  private final VirtualFile myFile;
  private final long myFileSize;

  public TooLargeContentException(@NotNull VirtualFile file, long fileSize) {
    myFile = file;
    myFileSize = fileSize;
  }

  @NotNull
  public VirtualFile getFile() {
    return myFile;
  }

  public long getFileSize() {
    return myFileSize;
  }
}