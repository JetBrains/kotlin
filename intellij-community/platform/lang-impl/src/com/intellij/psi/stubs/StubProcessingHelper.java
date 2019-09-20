// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.stubs;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.indexing.FileBasedIndex;
import gnu.trove.THashSet;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * Author: dmitrylomov
 */
public final class StubProcessingHelper extends StubProcessingHelperBase {
  private final ThreadLocal<Set<VirtualFile>> myFilesHavingProblems = new ThreadLocal<>();

  @Override
  protected void onInternalError(final VirtualFile file) {
    Set<VirtualFile> set = myFilesHavingProblems.get();
    if (set == null) myFilesHavingProblems.set(set = new THashSet<>());
    set.add(file);
    // requestReindex() may want to acquire write lock (for indices not requiring content loading)
    // thus, because here we are under read lock, need to use invoke later
    ApplicationManager.getApplication().invokeLater(() -> FileBasedIndex.getInstance().requestReindex(file), ModalityState.NON_MODAL);
  }

  @Nullable
  Set<VirtualFile> takeAccumulatedFilesWithIndexProblems() {
    Set<VirtualFile> filesWithProblems = myFilesHavingProblems.get();
    if (filesWithProblems != null) myFilesHavingProblems.set(null);
    return filesWithProblems;
  }
}
