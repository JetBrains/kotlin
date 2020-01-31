// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.hash;

import com.google.common.collect.Maps;
import com.intellij.psi.stubs.*;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.indexing.FileBasedIndexExtension;
import com.intellij.util.indexing.StorageException;
import com.intellij.util.indexing.impl.EmptyInputDataDiffBuilder;
import com.intellij.util.indexing.impl.InputData;
import com.intellij.util.indexing.impl.MapReduceIndex;
import com.intellij.util.indexing.impl.UpdateData;
import com.intellij.util.io.EnumeratorIntegerDescriptor;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StubHashBasedIndexGenerator extends HashBasedIndexGenerator<Integer, SerializedStubTree> {

  private final Map<StubIndexKey<?, ?>, HashBasedIndexGenerator<?, ?>> myStubIndexesGeneratorMap = new HashMap<>();

  private final Path myStubIndicesRoot;

  private StubHashBasedIndexGenerator(@NotNull SerializedStubTreeDataExternalizer externalizer,
                                      @NotNull StubUpdatingIndex index,
                                      @NotNull Path stubIndicesRoot,
                                      @NotNull List<StubIndexExtension<?, ?>> stubIndexExtensions) {
    super(EnumeratorIntegerDescriptor.INSTANCE, externalizer, index, stubIndicesRoot.getParent());
    myStubIndicesRoot = stubIndicesRoot;

    for (StubIndexExtension<?, ?> stubIndexExtension : stubIndexExtensions) {
      FileBasedIndexExtension<?, Void> extension = StubIndexImpl.wrapStubIndexExtension(stubIndexExtension);
      HashBasedIndexGenerator<?, Void> hashBasedIndexGenerator = createGenerator(extension);
      myStubIndexesGeneratorMap.put(stubIndexExtension.getKey(), hashBasedIndexGenerator);
    }
  }

  @NotNull
  public static StubHashBasedIndexGenerator create(@NotNull Path stubIndicesRoot,
                                                   @NotNull SerializationManagerEx serializationManager,
                                                   @NotNull List<StubIndexExtension<?, ?>> stubIndexExtensions) {
    StubForwardIndexExternalizer<?> forwardIndexExternalizer = StubForwardIndexExternalizer.createFileLocalExternalizer(serializationManager);
    return new StubHashBasedIndexGenerator(
      new SerializedStubTreeDataExternalizer(true, serializationManager, forwardIndexExternalizer),
      new StubUpdatingIndex(forwardIndexExternalizer, serializationManager),
      stubIndicesRoot,
      stubIndexExtensions
    );
  }

  @NotNull
  private <K, V> HashBasedIndexGenerator<K, V> createGenerator(FileBasedIndexExtension<K, V> extension) {
    return new HashBasedIndexGenerator<K, V>(extension.getKeyDescriptor(), extension.getValueExternalizer(), extension, myStubIndicesRoot) {
      @Override
      public @NotNull String getSharedIndexName() {
        return StubHashBasedIndexGenerator.this.getSharedIndexName() + "." + super.getSharedIndexName();
      }

      @Override
      public @NotNull String getSharedIndexVersion() {
        return StubHashBasedIndexGenerator.this.getSharedIndexVersion() + "." + super.getSharedIndexVersion();
      }
    };
  }

  @NotNull
  public List<HashBasedIndexGenerator<?, ?>> getStubGenerators() {
    return new ArrayList<>(myStubIndexesGeneratorMap.values());
  }

  @Override
  protected void visitInputData(int hashId, @NotNull InputData<Integer, SerializedStubTree> data) throws StorageException {
    super.visitInputData(hashId, data);
    SerializedStubTree tree = ContainerUtil.getFirstItem(data.getKeyValues().values());
    if (tree == null) return;
    Map<StubIndexKey, Map<Object, StubIdList>> map = tree.getStubIndicesValueMap();
    for (Map.Entry<StubIndexKey, Map<Object, StubIdList>> entry : map.entrySet()) {
      StubIndexKey key = entry.getKey();
      Map<Object, StubIdList> value = entry.getValue();
      MapReduceIndex index = (MapReduceIndex)myStubIndexesGeneratorMap.get(key).getIndex();
      Map<Object, Object> reducedValue = Maps.asMap(value.keySet(), k -> null);
      UpdateData<?, ?> updateData = new UpdateData(
        hashId,
        reducedValue,
        () -> new EmptyInputDataDiffBuilder(hashId),
        index.getExtension().getName(),
        null
      );
      index.updateWithMap(updateData);
    }
  }

  @Override
  public void openIndex() throws IOException {
    super.openIndex();
    for (HashBasedIndexGenerator<?, ?> generator : myStubIndexesGeneratorMap.values()) {
      generator.openIndex();
    }
  }

  @Override
  public void closeIndex() throws IOException {
    super.closeIndex();
    for (Map.Entry<StubIndexKey<?, ?>, HashBasedIndexGenerator<?, ?>> entry : myStubIndexesGeneratorMap.entrySet()) {
      entry.getValue().closeIndex();
    }
  }
}
