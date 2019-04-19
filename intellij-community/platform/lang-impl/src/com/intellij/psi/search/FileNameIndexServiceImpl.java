/*
 * Copyright 2000-2017 JetBrains s.r.o.
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

public class FileNameIndexServiceImpl implements FileNameIndexService {
  private final FileBasedIndex myIndex;

  public FileNameIndexServiceImpl(@NotNull FileBasedIndex index) {
    myIndex = index;
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
  public void processAllFileNames(@NotNull Processor<String> processor, @NotNull GlobalSearchScope scope, IdFilter filter) {
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