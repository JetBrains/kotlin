// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.stubs;

import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.indexing.StorageException;
import com.intellij.util.indexing.impl.InputDataDiffBuilder;
import com.intellij.util.indexing.impl.KeyValueUpdateProcessor;
import com.intellij.util.indexing.impl.RemovedKeyProcessor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

class StubsCumulativeInputDiffBuilder extends InputDataDiffBuilder<Integer, SerializedStubTree> {
  private final int myInputId;
  @Nullable // null if input was not indexed before
  private final IndexedStubs myIndexedStubs;

  StubsCumulativeInputDiffBuilder(int inputId,
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
      if (myIndexedStubs != null) {
        removeProcessor.process(myInputId, myInputId);
      }
      SerializedStubTree newSerializedStubTree = newData.get(myInputId);
      addProcessor.process(myInputId, newSerializedStubTree, myInputId);
      updateStubIndices(newSerializedStubTree);
    }
    else {
      removeProcessor.process(myInputId, myInputId);
      updateStubIndices(null);
    }
    return true;
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


  static void updateStubIndices(@NotNull final Collection<StubIndexKey> indexKeys,
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
