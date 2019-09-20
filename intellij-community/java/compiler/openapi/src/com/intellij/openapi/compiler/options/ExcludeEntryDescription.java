// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.openapi.compiler.options;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.pointers.VirtualFilePointer;
import com.intellij.openapi.vfs.pointers.VirtualFilePointerManager;
import org.jetbrains.annotations.Nullable;

public class ExcludeEntryDescription implements Disposable {
  private boolean myIsFile;
  private boolean myIncludeSubdirectories;
  private VirtualFilePointer myFilePointer;
  private final Disposable myParentDisposable;

  public ExcludeEntryDescription(String url, boolean includeSubdirectories, boolean isFile, Disposable parent) {
    myParentDisposable = parent;
    myFilePointer = VirtualFilePointerManager.getInstance().create(url, parent, null);
    myIncludeSubdirectories = includeSubdirectories;
    myIsFile = isFile;
  }

  public ExcludeEntryDescription(VirtualFile virtualFile, boolean includeSubdirectories, boolean isFile, Disposable parent) {
    this(virtualFile.getUrl(), includeSubdirectories, isFile, parent);
  }

  public ExcludeEntryDescription copy(Disposable parent) {
    return new ExcludeEntryDescription(getUrl(), myIncludeSubdirectories, myIsFile,parent);
  }

  void setPresentableUrl(String newUrl) {
    myFilePointer = VirtualFilePointerManager.getInstance().create(VfsUtilCore.pathToUrl(FileUtil.toSystemIndependentName(newUrl)), myParentDisposable, null);
    final VirtualFile file = getVirtualFile();
    if (file != null) {
      myIsFile = !file.isDirectory();
    }
  }

  public boolean isFile() {
    return myIsFile;
  }

  public String getUrl() {
    return myFilePointer.getUrl();
  }

  public String getPresentableUrl() {
    return myFilePointer.getPresentableUrl();
  }

  public boolean isIncludeSubdirectories() {
    return myIncludeSubdirectories;
  }

  void setIncludeSubdirectories(boolean includeSubdirectories) {
    myIncludeSubdirectories = includeSubdirectories;
  }

  @Nullable
  public VirtualFile getVirtualFile() {
    return myFilePointer.getFile();
  }

  public boolean isValid() {
    return myFilePointer.isValid();
  }

  public boolean equals(Object obj) {
    if(!(obj instanceof ExcludeEntryDescription)) {
      return false;
    }
    ExcludeEntryDescription entryDescription = (ExcludeEntryDescription)obj;
    if(entryDescription.myIsFile != myIsFile) {
      return false;
    }
    if(entryDescription.myIncludeSubdirectories != myIncludeSubdirectories) {
      return false;
    }
    return Comparing.equal(entryDescription.getUrl(), getUrl());
  }

  public int hashCode() {
    int result = (myIsFile ? 1 : 0);
    result = 31 * result + (myIncludeSubdirectories ? 1 : 0);
    result = 31 * result + getUrl().hashCode();
    return result;
  }

  @Override
  public void dispose() {
  }
}
