// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.hash;

import com.intellij.openapi.util.Computable;
import com.intellij.util.IntIntFunction;
import com.intellij.util.indexing.*;
import com.intellij.util.indexing.impl.AbstractUpdateData;
import com.intellij.util.indexing.impl.IndexStorage;
import com.intellij.util.indexing.impl.forward.MapForwardIndexAccessor;
import com.intellij.util.indexing.impl.forward.PersistentMapBasedForwardIndex;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Map;

public class FileContentHashIndex extends VfsAwareMapReduceIndex<Integer, Void, FileContent> {
  FileContentHashIndex(@NotNull FileContentHashIndexExtension extension, IndexStorage<Integer, Void> storage) throws IOException {
    super(extension,
          storage,
          new PersistentMapBasedForwardIndex(IndexInfrastructure.getInputIndexStorageFile(extension.getName())),
          new MapForwardIndexAccessor<>(new InputMapExternalizer<>(extension)), null, null);
  }

  @NotNull
  @Override
  protected Computable<Boolean> createIndexUpdateComputation(@NotNull AbstractUpdateData<Integer, Void> updateData) {
    return new HashIndexUpdateComputable(super.createIndexUpdateComputation(updateData), updateData.newDataIsEmpty());
  }

  public int getHashId(int fileId) throws StorageException {
    Map<Integer, Void> data = getIndexedFileData(fileId);
    if (data.isEmpty()) return 0;
    return data.keySet().iterator().next();
  }

  @NotNull
  IntIntFunction toHashIdToFileIdFunction() {
    return hash -> {
      try {
        ValueContainer<Void> data = getData(hash);
        assert data.size() == 1;
        return data.getValueIterator().getInputIdsIterator().next();
      }
      catch (StorageException e) {
        throw new RuntimeException(e);
      }
    };
  }

  final static class HashIndexUpdateComputable implements Computable<Boolean> {
    @NotNull
    private final Computable<Boolean> myUnderlying;
    private final boolean myEmptyInput;

    HashIndexUpdateComputable(@NotNull Computable<Boolean> underlying, boolean isEmptyInput) {myUnderlying = underlying;
      myEmptyInput = isEmptyInput;
    }

    boolean isEmptyInput() {
      return myEmptyInput;
    }

    @Override
    public Boolean compute() {
      return myUnderlying.compute();
    }
  }
}
