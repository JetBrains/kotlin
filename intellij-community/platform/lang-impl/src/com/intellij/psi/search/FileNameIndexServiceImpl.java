// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.search;

import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Processor;
import com.intellij.util.indexing.FileBasedIndex;
import com.intellij.util.indexing.IdFilter;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Set;

public final class FileNameIndexServiceImpl implements FileNameIndexService {
  private final FileBasedIndex myIndex;

  public FileNameIndexServiceImpl() {
    myIndex = FileBasedIndex.getInstance();
  }

  @NotNull
  @Override
  public Collection<VirtualFile> getVirtualFilesByName(Project project, @NotNull String name, @NotNull GlobalSearchScope scope, IdFilter filter) {
    Set<VirtualFile> files = new THashSet<>();
    myIndex.processValues(FilenameIndexImpl.NAME, name, null, (file, value) -> {
      files.add(file);
      return true;
    }, scope, filter);
    return files;
  }

  @Override
  public void processAllFileNames(@NotNull Processor<? super String> processor, @NotNull GlobalSearchScope scope, IdFilter filter) {
    myIndex.processAllKeys(FilenameIndexImpl.NAME, processor, scope, filter);
  }

  @NotNull
  @Override
  public Collection<VirtualFile> getFilesWithFileType(@NotNull FileType fileType, @NotNull GlobalSearchScope scope) {
    return myIndex.getContainingFiles(FileTypeIndexImpl.NAME, fileType, scope);
  }

  @Override
  public boolean processFilesWithFileType(@NotNull FileType fileType, @NotNull Processor<? super VirtualFile> processor, @NotNull GlobalSearchScope scope) {
    return myIndex.processValues(FileTypeIndexImpl.NAME, fileType, null, (file, value) -> processor.process(file), scope);
  }
}