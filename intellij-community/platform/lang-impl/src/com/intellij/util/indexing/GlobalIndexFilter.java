// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * Allows to exclude files from indexing, on a per-index basis.
 *
 * @author yole
 */
@ApiStatus.Experimental
public interface GlobalIndexFilter {
  /**
   * Returns true if the given file should be excluded from indexing by the given index.
   */
  boolean isExcludedFromIndex(@NotNull VirtualFile virtualFile, @NotNull IndexId<?, ?> indexId);

  int getVersion();

  boolean affectsIndex(@NotNull IndexId<?, ?> indexId);

  ExtensionPointName<GlobalIndexFilter> EP_NAME = ExtensionPointName.create("com.intellij.globalIndexFilter");

  /**
   * Returns true if the given file should be excluded from indexing by any of the registered filters.
   */
  static boolean isExcludedFromIndexViaFilters(@NotNull VirtualFile file, @NotNull IndexId<?, ?> indexId) {
    for (GlobalIndexFilter filter : EP_NAME.getExtensionList()) {
      if (filter.isExcludedFromIndex(file, indexId)) {
        return true;
      }
    }
    return false;
  }

  static int getFiltersVersion(@NotNull IndexId<?, ?> indexId) {
    int result = 0;
    for (GlobalIndexFilter extension: EP_NAME.getExtensionList()) {
      if (extension.affectsIndex(indexId)) {
        result += extension.getVersion();
      }
    }
    return result;
  }
}
