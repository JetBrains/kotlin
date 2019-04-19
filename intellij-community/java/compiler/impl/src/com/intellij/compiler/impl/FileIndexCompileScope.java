/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.compiler.impl;

import com.intellij.openapi.compiler.CompileScope;
import com.intellij.openapi.compiler.ExportableUserDataHolderBase;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.roots.FileIndex;
import com.intellij.openapi.vfs.VfsUtil;
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
  @NotNull
  public VirtualFile[] getFiles(final FileType fileType, final boolean inSourceOnly) {
    final List<VirtualFile> files = new ArrayList<>();
    final FileIndex[] fileIndices = getFileIndices();
    for (final FileIndex fileIndex : fileIndices) {
      fileIndex.iterateContent(new CompilerContentIterator(fileType, fileIndex, inSourceOnly, files));
    }
    return VfsUtil.toVirtualFileArray(files);
  }
}
