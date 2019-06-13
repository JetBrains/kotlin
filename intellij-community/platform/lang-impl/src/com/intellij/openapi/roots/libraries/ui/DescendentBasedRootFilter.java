// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.roots.libraries.ui;

import com.intellij.ide.highlighter.ArchiveFileType;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeRegistry;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

/**
 * Determines whether an archive or a directory can be used as a root of given type by analyzing its descending files; if there is at least one
 * file under it satisfying the given condition, it assumes that the original archive/directory can be used as a root of the given type.
 *
 * @author nik
 */
public class DescendentBasedRootFilter extends RootFilter {
  private final Predicate<? super VirtualFile> myCondition;

  public DescendentBasedRootFilter(OrderRootType rootType, boolean jarDirectory, String presentableRootTypeName, Predicate<? super VirtualFile> condition) {
    super(rootType, jarDirectory, presentableRootTypeName);
    myCondition = condition;
  }

  /**
   * @return filter which accepts file as a root if there is at least one file of {@code fileType} under it.
   */
  public static DescendentBasedRootFilter createFileTypeBasedFilter(OrderRootType rootType, boolean jarDirectory,
                                                                    @NotNull FileType fileType, String presentableRootTypeName) {
    return new DescendentBasedRootFilter(rootType, jarDirectory, presentableRootTypeName, file -> FileTypeRegistry.getInstance().isFileOfType(file, fileType));
  }

  @Override
  public boolean isAccepted(@NotNull VirtualFile rootCandidate, @NotNull final ProgressIndicator progressIndicator) {
    if (isJarDirectory()) {
      if (!rootCandidate.isDirectory() || !rootCandidate.isInLocalFileSystem()) {
        return false;
      }
      for (VirtualFile child : rootCandidate.getChildren()) {
        if (!child.isDirectory() && FileTypeRegistry.getInstance().isFileOfType(child, ArchiveFileType.INSTANCE)) {
          final VirtualFile jarRoot = JarFileSystem.getInstance().getJarRootForLocalFile(child);
          if (jarRoot != null && containsFileOfType(jarRoot, progressIndicator)) {
            return true;
          }
        }
      }
      return false;
    }
    else {
      return containsFileOfType(rootCandidate, progressIndicator);
    }
  }

  private boolean containsFileOfType(VirtualFile rootCandidate, final ProgressIndicator progressIndicator) {
    return !VfsUtilCore.processFilesRecursively(rootCandidate, virtualFile -> {
      progressIndicator.checkCanceled();
      if (virtualFile.isDirectory()) {
        progressIndicator.setText2(virtualFile.getPath());
        return true;
      }
      return !myCondition.test(virtualFile);
    });
  }
}
