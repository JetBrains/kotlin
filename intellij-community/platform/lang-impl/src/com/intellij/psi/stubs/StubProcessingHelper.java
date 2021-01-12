// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.stubs;

import com.intellij.openapi.application.AppUIExecutor;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileWithId;
import com.intellij.psi.PsiElement;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.indexing.FileBasedIndex;
import com.intellij.util.indexing.StorageException;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

public final class StubProcessingHelper extends StubProcessingHelperBase {
  private final ThreadLocal<Set<VirtualFile>> myFilesHavingProblems = new ThreadLocal<>();

  @Nullable
  public <Key, Psi extends PsiElement> StubIdList retrieveStubIdList(@NotNull StubIndexKey<Key, Psi> indexKey,
                                                                     @NotNull Key key,
                                                                     @NotNull VirtualFile file,
                                                                     @NotNull Project project) {
    int id = ((VirtualFileWithId)file).getId();
    try {
      Map<Integer, SerializedStubTree> data = StubIndexImpl.getStubUpdatingIndex().getIndexedFileData(id);
      if (data.size() != 1) {
        LOG.error("Stub index points to a file (" + getFileTypeInfo(file, project) + ") without indexed stub tree");
        onInternalError(file);
        return null;
      }
      SerializedStubTree tree = data.values().iterator().next();
      StubIdList stubIdList = tree.restoreIndexedStubs(indexKey, key);
      if (stubIdList == null) {
        String mainMessage = "Stub ids not found for key in index = " + indexKey.getName() + ", " + getFileTypeInfo(file, project);
        String additionalMessage;
        if (ApplicationManager.getApplication().isUnitTestMode()) {
          Map<StubIndexKey<?, ?>, Map<Object, StubIdList>> map = null;
          try {
            tree.restoreIndexedStubs();
            map = tree.getStubIndicesValueMap();
          } catch (Exception ignored) {}
          additionalMessage = ", file " + file.getPath() + ", for key " + key + " existing map " + map;
        }
        else {
          additionalMessage = "";
        }
        LOG.error(mainMessage + additionalMessage);
        onInternalError(file);
      }
      return stubIdList;
    }
    catch (StorageException | IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  protected void onInternalError(final VirtualFile file) {
    Set<VirtualFile> set = myFilesHavingProblems.get();
    if (set == null) myFilesHavingProblems.set(set = new THashSet<>());
    set.add(file);
    // requestReindex() may want to acquire write lock (for indices not requiring content loading)
    // thus, because here we are under read lock, need to use invoke later
    AppUIExecutor.onWriteThread(ModalityState.NON_MODAL).later().submit(() -> FileBasedIndex.getInstance().requestReindex(file));
  }

  @Nullable
  Set<VirtualFile> takeAccumulatedFilesWithIndexProblems() {
    Set<VirtualFile> filesWithProblems = myFilesHavingProblems.get();
    if (filesWithProblems != null) myFilesHavingProblems.set(null);
    return filesWithProblems;
  }

  @TestOnly
  boolean areAllProblemsProcessedInTheCurrentThread() {
    return ContainerUtil.isEmpty(myFilesHavingProblems.get());
  }
}
