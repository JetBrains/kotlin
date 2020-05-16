// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.stubs;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.impl.DebugUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.indexing.FileBasedIndexImpl;
import com.intellij.util.indexing.StorageException;
import com.intellij.util.indexing.impl.DirectInputDataDiffBuilder;
import com.intellij.util.indexing.impl.IndexDebugProperties;
import com.intellij.util.indexing.impl.KeyValueUpdateProcessor;
import com.intellij.util.indexing.impl.RemovedKeyProcessor;
import one.util.streamex.IntStreamEx;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

class StubCumulativeInputDiffBuilder extends DirectInputDataDiffBuilder<Integer, SerializedStubTree> {
  private static final Logger LOG = Logger.getInstance(SerializedStubTree.class);
  private final int myInputId;
  @Nullable
  private final SerializedStubTree myCurrentTree;

  StubCumulativeInputDiffBuilder(int inputId, @Nullable SerializedStubTree currentTree) {
    super(inputId);
    myInputId = inputId;
    myCurrentTree = currentTree;
  }

  @Override
  public boolean differentiate(@NotNull Map<Integer, SerializedStubTree> newData,
                               @NotNull KeyValueUpdateProcessor<? super Integer, ? super SerializedStubTree> addProcessor,
                               @NotNull KeyValueUpdateProcessor<? super Integer, ? super SerializedStubTree> updateProcessor,
                               @NotNull RemovedKeyProcessor<? super Integer> removeProcessor) throws StorageException {
    if (!newData.isEmpty()) {
      SerializedStubTree newSerializedStubTree = newData.values().iterator().next();
      if (myCurrentTree != null) {
        if (treesAreEqual(newSerializedStubTree, myCurrentTree)) return false;
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

  @Override
  public @NotNull Collection<Integer> getKeys() {
    return myCurrentTree != null ? Collections.singleton(myInputId) : Collections.emptySet();
  }

  private static boolean treesAreEqual(@NotNull SerializedStubTree newSerializedStubTree,
                                       @NotNull SerializedStubTree currentTree) {
    return Arrays.equals(currentTree.getTreeHash(), newSerializedStubTree.getTreeHash()) &&
           treesAreReallyEqual(newSerializedStubTree, currentTree);
  }

  private static boolean treesAreReallyEqual(@NotNull SerializedStubTree newSerializedStubTree,
                                             @NotNull SerializedStubTree currentTree) {
    if (newSerializedStubTree.equals(currentTree)) {
      return true;
    }
    if (IndexDebugProperties.DEBUG) {
      reportStubTreeHashCollision(newSerializedStubTree, currentTree);
    }
    return false;
  }

  private void updateStubIndices(@Nullable SerializedStubTree newTree) {
    Map<StubIndexKey<?, ?>, Map<Object, StubIdList>> oldForwardIndex =
      myCurrentTree == null ? Collections.emptyMap() : myCurrentTree.getStubIndicesValueMap();

    Map<StubIndexKey<?, ?>, Map<Object, StubIdList>> newForwardIndex =
      newTree == null ? Collections.emptyMap() : newTree.getStubIndicesValueMap();

    Collection<StubIndexKey<?, ?>> affectedIndexes =
      ContainerUtil.union(oldForwardIndex.keySet(), newForwardIndex.keySet());

    if (FileBasedIndexImpl.DO_TRACE_STUB_INDEX_UPDATE) {
      StubIndexImpl.LOG.info("stub indexes" + (newTree == null ? "deletion" : "update") + ": file = " + myInputId + " indexes " + affectedIndexes);
    }

    StubIndexImpl stubIndex = (StubIndexImpl)StubIndex.getInstance();
    //noinspection rawtypes
    for (StubIndexKey key : affectedIndexes) {
      // StubIdList-s are ignored.
      Set<Object> oldKeys = oldForwardIndex.getOrDefault(key, Collections.emptyMap()).keySet();
      Set<Object> newKeys = newForwardIndex.getOrDefault(key, Collections.emptyMap()).keySet();

      //noinspection unchecked
      stubIndex.updateIndex(key, myInputId, oldKeys, newKeys);
    }
  }

  private static void reportStubTreeHashCollision(@NotNull SerializedStubTree newTree,
                                                  @NotNull SerializedStubTree existingTree) {
    String oldTreeDump = "\nexisting tree " + dumpStub(existingTree);
    String newTreeDump = "\nnew tree " + dumpStub(newTree);
    byte[] hash = newTree.getTreeHash();
    LOG.info("Stub tree hashing collision. " +
             "Different trees have the same hash = " + toHexString(hash, hash.length) + ". " +
             oldTreeDump + newTreeDump, new Exception());
  }

  private static String toHexString(byte[] hash, int length) {
    return IntStreamEx.of(hash).limit(length).mapToObj(b -> String.format("%02x", b & 0xFF)).joining();
  }

  @NotNull
  private static String dumpStub(@NotNull SerializedStubTree tree) {
    String deserialized;
    try {
      deserialized = "stub: " + DebugUtil.stubTreeToString(tree.getStub());
    }
    catch (SerializerNotFoundException e) {
      LOG.error(e);
      deserialized = "error while stub deserialization: " + e.getMessage();
    }
    return deserialized + "\n bytes: " + toHexString(tree.myTreeBytes, tree.myTreeByteLength);
  }
}
