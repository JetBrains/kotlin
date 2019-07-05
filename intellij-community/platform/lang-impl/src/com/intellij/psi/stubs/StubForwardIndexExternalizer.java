// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.stubs;

import com.intellij.openapi.progress.ProgressManager;
import com.intellij.util.indexing.ID;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.DataInputOutputUtil;
import gnu.trove.THashMap;
import org.jetbrains.annotations.NotNull;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

public abstract class StubForwardIndexExternalizer<StubKeySerializationState> implements DataExternalizer<Map<StubIndexKey, Map<Object, StubIdList>>> {
  private volatile boolean myEnsuredStubElementTypesLoaded;

  protected abstract StubKeySerializationState createStubIndexKeySerializationState(@NotNull DataOutput out, @NotNull Set<StubIndexKey> set) throws IOException;

  protected abstract void writeStubIndexKey(@NotNull DataOutput out, @NotNull StubIndexKey key, StubKeySerializationState state) throws IOException;

  protected abstract StubKeySerializationState createStubIndexKeySerializationState(@NotNull DataInput input, int stubIndexKeyCount) throws IOException;

  protected abstract ID<?, ?> readStubIndexKey(@NotNull DataInput input, StubKeySerializationState stubKeySerializationState) throws IOException;

  @Override
  public void save(@NotNull DataOutput out, Map<StubIndexKey, Map<Object, StubIdList>> indexedStubs) throws IOException {

    DataInputOutputUtil.writeINT(out, indexedStubs.size());
    if (!indexedStubs.isEmpty()) {
      StubKeySerializationState stubKeySerializationState = createStubIndexKeySerializationState(out, indexedStubs.keySet());

      StubIndexImpl stubIndex = (StubIndexImpl)StubIndex.getInstance();
      for (StubIndexKey stubIndexKey : indexedStubs.keySet()) {
        writeStubIndexKey(out, stubIndexKey, stubKeySerializationState);
        Map<Object, StubIdList> map = indexedStubs.get(stubIndexKey);
        stubIndex.serializeIndexValue(out, stubIndexKey, map);
      }
    }
  }

  @Override
  public Map<StubIndexKey, Map<Object, StubIdList>> read(@NotNull DataInput in) throws IOException {
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
      StubKeySerializationState stubKeySerializationState = createStubIndexKeySerializationState(in, stubIndicesValueMapSize);
      for (int i = 0; i < stubIndicesValueMapSize; ++i) {
        ID<Object, ?> indexKey = (ID<Object, ?>)readStubIndexKey(in, stubKeySerializationState);
        if (indexKey instanceof StubIndexKey) { // indexKey can be ID in case of removed index
          StubIndexKey<Object, ?> stubIndexKey = (StubIndexKey<Object, ?>)indexKey;
          stubIndicesValueMap.put(stubIndexKey, stubIndex.deserializeIndexValue(in, stubIndexKey));
        }
      }
      return stubIndicesValueMap;
    }
    return Collections.emptyMap();
  }

  static class IdeStubForwardIndexesExternalizer extends StubForwardIndexExternalizer<Void> {
    static final IdeStubForwardIndexesExternalizer INSTANCE = new IdeStubForwardIndexesExternalizer();

    @Override
    protected void writeStubIndexKey(@NotNull DataOutput out, @NotNull StubIndexKey key, Void aVoid) throws IOException {
      DataInputOutputUtil.writeINT(out, key.getUniqueId());
    }

    @Override
    protected Void createStubIndexKeySerializationState(@NotNull DataOutput out, @NotNull Set<StubIndexKey> set) {
      return null;
    }

    @Override
    protected ID<?, ?> readStubIndexKey(@NotNull DataInput input, Void aVoid) throws IOException {
      return ID.findById(DataInputOutputUtil.readINT(input));
    }

    @Override
    protected Void createStubIndexKeySerializationState(@NotNull DataInput input, int stubIndexKeyCount) {
      return null;
    }
  }
}
