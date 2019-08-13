// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.compiler.backwardRefs;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.compiler.options.ExcludeEntryDescription;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.ManagingFS;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class ExcludedFromCompileFilesUtil {
  static GlobalSearchScope getExcludedFilesScope(@NotNull ExcludeEntryDescription[] descriptions,
                                                 @NotNull Set<FileType> fileTypes,
                                                 @NotNull Project project,
                                                 @NotNull ProjectFileIndex fileIndex) {
    ManagingFS fs = ManagingFS.getInstance();
    final Collection<VirtualFile> excludedFiles = Stream.of(descriptions)
      .flatMap(description -> {
        final VirtualFile file = description.getVirtualFile();
        if (file == null) return Stream.empty();
        if (description.isFile()) {
          return Stream.of(file);
        }
        else if (description.isIncludeSubdirectories()) {
          final Stream.Builder<VirtualFile> builder = Stream.builder();
          VfsUtilCore.iterateChildrenRecursively(file, f -> !f.isDirectory() || fs.areChildrenLoaded(f), f -> {
            builder.accept(f);
            return true;
          });
          return builder.build();
        }
        else {
          return fs.areChildrenLoaded(file) ? Stream.of(file.getChildren()) : Stream.empty();
        }
      })
      .filter(f -> !f.isDirectory() && fileTypes.contains(f.getFileType()) && ReadAction.compute(() -> !project.isDisposed() && fileIndex.isInSourceContent(f)))
      .collect(Collectors.toList());

    return GlobalSearchScope.filesWithoutLibrariesScope(project, excludedFiles);
  }
}
