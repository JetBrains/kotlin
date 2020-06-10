// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.contentQueue;

import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

public interface CachedFileContentLoader {
  @NotNull
  CachedFileContent loadContent(@NotNull VirtualFile file) throws ProcessCanceledException,
                                                                  TooLargeContentException,
                                                                  FailedToLoadContentException;
}