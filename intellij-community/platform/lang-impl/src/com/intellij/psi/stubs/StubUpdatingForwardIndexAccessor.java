// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.stubs;

import com.intellij.openapi.progress.ProgressManager;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.indexing.FileContent;
import com.intellij.util.indexing.ID;
import com.intellij.util.indexing.impl.InputDataDiffBuilder;
import com.intellij.util.indexing.impl.forward.AbstractForwardIndexAccessor;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.DataInputOutputUtil;
import gnu.trove.THashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;

class StubUpdatingForwardIndexAccessor extends AbstractForwardIndexAccessor<Integer, SerializedStubTree, IndexedStubs , FileContent> {
  StubUpdatingForwardIndexAccessor() {super(new DataExternalizer<IndexedStubs>() {
    private volatile boolean myEnsuredStubElementTypesLoaded;

    @Override
    public void save(@NotNull DataOutput out, IndexedStubs indexedStubs) throws IOException {
      DataInputOutputUtil.writeINT(out, indexedStubs.getFileId());
      Map<StubIndexKey, Map<Object, StubIdList>> stubIndicesValueMap = indexedStubs.getStubIndicesValueMap();
      DataInputOutputUtil.writeINT(out, stubIndicesValueMap.size());
      if (!stubIndicesValueMap.isEmpty()) {
        StubIndexImpl stubIndex = (StubIndexImpl)StubIndex.getInstance();

        for (StubIndexKey stubIndexKey : stubIndicesValueMap.keySet()) {
          DataInputOutputUtil.writeINT(out, stubIndexKey.getUniqueId());
          Map<Object, StubIdList> map = stubIndicesValueMap.get(stubIndexKey);
          stubIndex.serializeIndexValue(out, stubIndexKey, map);
        }
      }
    }

    @Override
    public IndexedStubs read(@NotNull DataInput in) throws IOException {
      int fileId = DataInputOutputUtil.readINT(in);
      if (!myEnsuredStubElementTypesLoaded) {
        ProgressManager.getInstance().executeNonCancelableSection(() -> {
          SerializationManager.getInstance().initSerializers();
          StubIndexImpl.initExtensions();
        });
        myEnsuredStubElementTypesLoaded = true;
      }
      int stubIndicesValueMapSize = DataInputOutputUtil.readINT(in);
      if (stubIndicesValueMapSize > 0) {
        THashMap<StubIndexKey, Map<Object, StubIdList>> stubIndicesValueMap = new THashMap<>(stubIndicesValueMapSize);
        StubIndexImpl stubIndex = (StubIndexImpl)StubIndex.getInstance();

        for (int i = 0; i < stubIndicesValueMapSize; ++i) {
          int stubIndexId = DataInputOutputUtil.readINT(in);
          ID<Object, ?> indexKey = (ID<Object, ?>)ID.findById(stubIndexId);
          if (indexKey instanceof StubIndexKey) { // indexKey can be ID in case of removed index
            StubIndexKey<Object, ?> stubIndexKey = (StubIndexKey<Object, ?>)indexKey;
            stubIndicesValueMap.put(stubIndexKey, stubIndex.deserializeIndexValue(in, stubIndexKey));
          }
        }
        return new IndexedStubs(fileId, stubIndicesValueMap);
      }
      return new IndexedStubs(fileId, Collections.emptyMap());
    }
  });}

  @Override
  public IndexedStubs convertToDataType(@Nullable Map<Integer, SerializedStubTree> map,
                                                                      @Nullable FileContent content) {
    return getIndexedStubs(map);
  }

  @Override
  protected InputDataDiffBuilder<Integer, SerializedStubTree> createDiffBuilder(int inputId,
                                                                                @Nullable IndexedStubs inputData) {
    return new StubsCumulativeInputDiffBuilder(inputId, inputData);
  }

  @Nullable
  static IndexedStubs getIndexedStubs(@Nullable Map<? extends Integer, ? extends SerializedStubTree> map) {
    SerializedStubTree serializedStubTree = ContainerUtil.getFirstItem(ContainerUtil.notNullize(map).values());
    return serializedStubTree == null ? null : serializedStubTree.getIndexedStubs();
  }
}
