// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.hash;

import com.google.common.collect.Maps;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
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

  public StubHashBasedIndexGenerator(@NotNull Path outRoot) {
    super(
      EnumeratorIntegerDescriptor.INSTANCE,
      new SerializedStubTreeDataExternalizer(true, null, StubForwardIndexExternalizer.FileLocalStubForwardIndexExternalizer.INSTANCE),
      new StubUpdatingIndex(StubForwardIndexExternalizer.FileLocalStubForwardIndexExternalizer.INSTANCE),
      outRoot
    );

    myStubIndicesRoot = outRoot.resolve(StringUtil.toLowerCase(StubUpdatingIndex.INDEX_ID.getName()));

    for (StubIndexExtension<?, ?> stubIndexExtension : StubIndexExtension.EP_NAME.getExtensionList()) {
      FileBasedIndexExtension<?, Void> extension = StubIndexImpl.wrapStubIndexExtension(stubIndexExtension);
      HashBasedIndexGenerator<?, Void> hashBasedIndexGenerator = createGenerator(extension);
      myStubIndexesGeneratorMap.put(stubIndexExtension.getKey(), hashBasedIndexGenerator);
    }
  }

  @NotNull
  private <K, V> HashBasedIndexGenerator<K, V> createGenerator(FileBasedIndexExtension<K, V> extension) {
    return new HashBasedIndexGenerator<>(extension.getKeyDescriptor(), extension.getValueExternalizer(), extension, myStubIndicesRoot);
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
    ((SerializationManagerEx)SerializationManager.getInstance()).flushNameStorage();

    FileUtil.copyDir(PathManager.getIndexRoot(), myStubIndicesRoot.toFile(), f -> f.getName().startsWith("rep.names"));
  }
}
