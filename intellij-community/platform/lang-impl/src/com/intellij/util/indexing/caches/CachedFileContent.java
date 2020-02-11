// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.caches;

import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ArrayUtilRt;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public final class CachedFileContent extends UserDataHolderBase {
  private final VirtualFile myVirtualFile;
  private byte[] myCachedBytes;
  private long myCachedLength = -1;
  private long myCachedTimeStamp = -1;
  private Boolean myCachedWritable;

  public CachedFileContent(@NotNull VirtualFile virtualFile) {
    myVirtualFile = virtualFile;
  }

  public byte @NotNull [] getBytesOrEmptyArray() {
    try {
      return getBytes();
    } catch (IOException e) {
      return ArrayUtilRt.EMPTY_BYTE_ARRAY;
    }
  }

  public byte @NotNull [] getBytes() throws IOException {
    if (myCachedBytes == null) {
      if (myVirtualFile.isValid()) {
        myCachedTimeStamp = myVirtualFile.getTimeStamp();
        myCachedBytes = myVirtualFile.contentsToByteArray(false);
      }
      else {
        myCachedTimeStamp = -1;
        myCachedBytes = ArrayUtilRt.EMPTY_BYTE_ARRAY;
      }
    }
    return myCachedBytes;
  }

  public void setEmptyContent() {
    myCachedBytes = ArrayUtilRt.EMPTY_BYTE_ARRAY;
    myCachedLength = 0;
  }

  @NotNull
  public VirtualFile getVirtualFile() {
    return myVirtualFile;
  }

  public long getLength() {
    if (myCachedLength == -1) {
      myCachedLength = myVirtualFile.getLength();
    }
    return myCachedLength;
  }

  public long getTimeStamp() {
    if (myCachedTimeStamp == -1) {
      myCachedTimeStamp = myVirtualFile.getTimeStamp();
    }
    return myCachedTimeStamp;
  }

  public boolean isWritable() {
    if (myCachedWritable == null) {
      myCachedWritable = myVirtualFile.isWritable();
    }
    return myCachedWritable == Boolean.TRUE;
  }
}