// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.caches;

import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface CachedFileContentQueue {

  /**
   * Loads the next content and wraps it in a token that must be disposed by calling either
   * {@link CachedFileContentToken#release()} or {@link CachedFileContentToken#pushBack()}
   * when the content processing has been finished.<br/>
   * This method blocks the current thread until there is memory available for loading the next content.
   *
   * @param indicator indicator to cancel loading the next content
   * @return the next loaded content, or {@code null} if all files are processed
   * @throws TooLargeContentException     if the next file content can't be stored in memory
   * @throws FailedToLoadContentException if loading has failed with unexpected exception
   * @throws ProcessCanceledException     if loading has been cancelled
   */
  @Nullable
  CachedFileContentToken loadNextContent(@NotNull ProgressIndicator indicator) throws FailedToLoadContentException,
                                                                                      TooLargeContentException,
                                                                                      ProcessCanceledException;
}