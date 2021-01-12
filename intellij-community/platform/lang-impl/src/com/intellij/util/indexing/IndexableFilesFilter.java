// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * This EP is used to indicate if file should be indexed or not.
 * It could be used in dynamic indexing, e.g. if file should not be indexed on some indexing phase but should be processed later.
 * This EP is very experimental and likely to be removed one day. Please try to avoid using it.
 */
@ApiStatus.Experimental
@ApiStatus.Internal
public interface IndexableFilesFilter {

  ExtensionPointName<IndexableFilesFilter> EP_NAME = ExtensionPointName.create("com.intellij.indexableFilesFilter");

  /**
   * @param file file is going to be indexed
   * @return true if file should be indexed, returned value is not saved, it is asked on every indexing.
   */
  boolean shouldIndex(@NotNull VirtualFile file);
}
