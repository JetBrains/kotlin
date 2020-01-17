// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.ByteArraySequence;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileVisitor;
import com.intellij.util.indexing.impl.forward.AbstractForwardIndexAccessor;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Map;

@ApiStatus.Internal
public class IndexExportUtil {
  private static final Logger LOG = Logger.getInstance(IndexExportUtil.class);

  public static <K, V> void processInputDataRecursively(@NotNull VirtualFile file,
                                                        @NotNull ID<K, V> indexId,
                                                        @NotNull InputDataVisitor visitor,
                                                        @Nullable Project project) {
    FileBasedIndexImpl fileBasedIndex = (FileBasedIndexImpl)FileBasedIndex.getInstance();
    fileBasedIndex.ensureUpToDate(indexId, project, null, null);
    UpdatableIndex<K, V, FileContent> index = fileBasedIndex.getIndex(indexId);
    InputMapExternalizer<K, V> inputMapExternalizer = new InputMapExternalizer<>(index.getExtension());
    VfsUtilCore.visitChildrenRecursively(file, new VirtualFileVisitor<Void>() {
      @Override
      public boolean visitFile(@NotNull VirtualFile file1) {
        try {
          Map<K, V> data = index.getIndexedFileData(FileBasedIndex.getFileId(file1));
          ByteArraySequence bytes = data.isEmpty()
                                    ? null
                                    : AbstractForwardIndexAccessor.serializeToByteSeq(data, inputMapExternalizer, 8);
          return visitor.visit(file1, bytes);
        }
        catch (StorageException | IOException e) {
          LOG.error(e);
          return false;
        }
      }
    });
  }

  @FunctionalInterface
  interface InputDataVisitor {
    boolean visit(@NotNull VirtualFile file, @Nullable("associated data is empty") ByteArraySequence serializedInput);
  }
}
