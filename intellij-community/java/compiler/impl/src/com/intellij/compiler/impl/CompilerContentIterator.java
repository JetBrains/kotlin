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

import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.fileTypes.FileTypeRegistry;
import com.intellij.openapi.roots.ContentIterator;
import com.intellij.openapi.roots.FileIndex;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public class CompilerContentIterator implements ContentIterator {
  final FileTypeManager fileTypeManager = FileTypeManager.getInstance();
  private final FileType myFileType;
  private final FileIndex myFileIndex;
  private final boolean myInSourceOnly;
  private final Collection<? super VirtualFile> myFiles;

  public CompilerContentIterator(FileType fileType, FileIndex fileIndex, boolean inSourceOnly, Collection<? super VirtualFile> files) {
    myFileType = fileType;
    myFileIndex = fileIndex;
    myInSourceOnly = inSourceOnly;
    myFiles = files;
  }

  @Override
  public boolean processFile(@NotNull VirtualFile fileOrDir) {
    if (fileOrDir.isDirectory()) return true;
    if (!fileOrDir.isInLocalFileSystem()) return true;
    if (myInSourceOnly && !myFileIndex.isInSourceContent(fileOrDir)) return true;
    if (myFileType == null || FileTypeRegistry.getInstance().isFileOfType(fileOrDir, myFileType)) {
      myFiles.add(fileOrDir);
    }
    return true;
  }
}
