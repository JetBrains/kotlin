// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.stubs;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.ManagingFS;
import com.intellij.util.indexing.FileBasedIndex;
import com.intellij.util.indexing.FileBasedIndexImpl;
import com.intellij.util.indexing.StorageException;
import com.intellij.util.indexing.impl.DebugAssertions;
import com.intellij.util.indexing.impl.InputDataDiffBuilder;
import com.intellij.util.indexing.impl.KeyValueUpdateProcessor;
import com.intellij.util.indexing.impl.RemovedKeyProcessor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

class StubCumulativeInputDiffBuilder extends InputDataDiffBuilder<Integer, SerializedStubTree> {
  private static final Logger LOG = Logger.getInstance(StubCumulativeInputDiffBuilder.class);
  private final int myInputId;
  @Nullable // null if input was not indexed before
  private final IndexedStubs myIndexedStubs;

  StubCumulativeInputDiffBuilder(int inputId,
                                 @Nullable IndexedStubs indexedStubs) {
    super(inputId);
    myInputId = inputId;
    myIndexedStubs = indexedStubs;
  }

  @Override
  public boolean differentiate(@NotNull Map<Integer, SerializedStubTree> newData,
                               @NotNull KeyValueUpdateProcessor<? super Integer, ? super SerializedStubTree> addProcessor,
                               @NotNull KeyValueUpdateProcessor<? super Integer, ? super SerializedStubTree> updateProcessor,
                               @NotNull RemovedKeyProcessor<? super Integer> removeProcessor) throws StorageException {
    if (newData.containsKey(myInputId)) {
      SerializedStubTree newSerializedStubTree = newData.get(myInputId);
      if (myIndexedStubs != null) {
        byte[] currentHash = myIndexedStubs.getStubTreeHash();
        final boolean[] treesAreEqual = new boolean[1];
        final StorageException[] exception = new StorageException[1];
        ProgressManager.getInstance().executeNonCancelableSection(() -> {
          try {
            treesAreEqual[0] = treesAreEqual(newSerializedStubTree, currentHash);
          }
          catch (StorageException e) {
            exception[0] = e;
          }
        });
        if (treesAreEqual[0]) return false;
        if (exception[0] != null) {
          throw exception[0];
        }
        removeProcessor.process(myInputId, myInputId);
      }
      addProcessor.process(myInputId, newSerializedStubTree, myInputId);
      updateStubIndices(newSerializedStubTree);
    }
    else {
      removeProcessor.process(myInputId, myInputId);
      updateStubIndices(null);
    }
    return true;
  }

  private boolean treesAreEqual(@NotNull SerializedStubTree newSerializedStubTree, @NotNull byte[] currentHash) throws StorageException {
    return Arrays.equals(currentHash, newSerializedStubTree.getIndexedStubs().getStubTreeHash()) &&
           treesAreReallyEqual(newSerializedStubTree, currentHash);
  }

  private boolean treesAreReallyEqual(@NotNull SerializedStubTree newSerializedStubTree, @NotNull byte[] hash) throws StorageException {
    Map<Integer, SerializedStubTree>
      data = ((FileBasedIndexImpl)FileBasedIndex.getInstance()).getIndex(StubUpdatingIndex.INDEX_ID).getIndexedFileData(myInputId);
    if (data.isEmpty()) {
      LOG.error("inconsistent \'" + StubUpdatingIndex.INDEX_ID.getName() + "\' index storage & forward-index. Serialized stub tree isn't present");
    }
    else if (data.size() != 1) {
      LOG.error(StubUpdatingIndex.INDEX_ID.getName() + " should contain only one tree per file: " + getIndexingFileName());
      return false;
    }
    SerializedStubTree storedTree = data.values().iterator().next();
    if (storedTree == null) {
      LOG.error("nullable trees are not allowed but found in " + getIndexingFileName());
      return false;
    }
    if (newSerializedStubTree.equals(storedTree)) {
      return true;
    }
    if (DebugAssertions.DEBUG) {
      SerializedStubTree.reportStubTreeHashCollision(newSerializedStubTree, storedTree, hash);
    }
    return false;
  }

  private String getIndexingFileName() {
    VirtualFile file = ManagingFS.getInstance().findFileById(myInputId);
    return file == null ? null : file.getName();
  }

  private void updateStubIndices(@Nullable SerializedStubTree newSerializedStubTree) {
    Map<StubIndexKey, Map<Object, StubIdList>> previousStubIndicesValueMap = myIndexedStubs == null
                                                                             ? Collections.emptyMap()
                                                                             : myIndexedStubs.getStubIndicesValueMap();
    Map<StubIndexKey, Map<Object, StubIdList>> newStubIndicesValueMap = newSerializedStubTree == null
                                                                        ? Collections.emptyMap()
                                                                        : newSerializedStubTree.getIndexedStubs().getStubIndicesValueMap();
    updateStubIndices(
      getAffectedIndices(previousStubIndicesValueMap, newStubIndicesValueMap),
      myInputId,
      previousStubIndicesValueMap,
      newStubIndicesValueMap
    );
  }

  private static void updateStubIndices(@NotNull final Collection<StubIndexKey> indexKeys,
                                        final int inputId,
                                        @NotNull final Map<StubIndexKey, Map<Object, StubIdList>> oldStubTree,
                                        @NotNull final Map<StubIndexKey, Map<Object, StubIdList>> newStubTree) {
    final StubIndexImpl stubIndex = (StubIndexImpl)StubIndex.getInstance();
    for (StubIndexKey key : indexKeys) {
      final Map<Object, StubIdList> oldMap = oldStubTree.get(key);
      final Map<Object, StubIdList> newMap = newStubTree.get(key);

      final Map<Object, StubIdList> _oldMap = oldMap != null ? oldMap : Collections.emptyMap();
      final Map<Object, StubIdList> _newMap = newMap != null ? newMap : Collections.emptyMap();

      stubIndex.updateIndex(key, inputId, _oldMap, _newMap);
    }
  }

  @NotNull
  private static Collection<StubIndexKey> getAffectedIndices(@NotNull final Map<StubIndexKey, Map<Object, StubIdList>> oldStubTree,
                                                             @NotNull final Map<StubIndexKey, Map<Object, StubIdList>> newStubTree) {
    Set<StubIndexKey> allIndices = new HashSet<>();
    allIndices.addAll(oldStubTree.keySet());
    allIndices.addAll(newStubTree.keySet());
    return allIndices;
  }
}
