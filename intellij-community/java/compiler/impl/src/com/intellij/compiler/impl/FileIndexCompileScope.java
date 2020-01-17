// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.compiler.impl;

import com.intellij.openapi.compiler.CompileScope;
import com.intellij.openapi.compiler.ExportableUserDataHolderBase;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.roots.FileIndex;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Eugene Zhuravlev
 */
public abstract class FileIndexCompileScope extends ExportableUserDataHolderBase implements CompileScope {

  protected abstract FileIndex[] getFileIndices();

  @Override
  public VirtualFile @NotNull [] getFiles(final FileType fileType, final boolean inSourceOnly) {
    final List<VirtualFile> files = new ArrayList<>();
    final FileIndex[] fileIndices = getFileIndices();
    for (final FileIndex fileIndex : fileIndices) {
      fileIndex.iterateContent(new CompilerContentIterator(fileType, fileIndex, inSourceOnly, files));
    }
    return VfsUtilCore.toVirtualFileArray(files);
  }
}
