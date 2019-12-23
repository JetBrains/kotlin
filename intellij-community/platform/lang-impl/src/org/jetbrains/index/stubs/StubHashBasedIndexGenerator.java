// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.index.stubs;

import com.google.common.collect.Maps;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.stubs.*;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.indexing.FileBasedIndexExtension;
import com.intellij.util.indexing.StorageException;
import com.intellij.util.indexing.hash.HashBasedIndexGenerator;
import com.intellij.util.indexing.impl.EmptyInputDataDiffBuilder;
import com.intellij.util.indexing.impl.InputData;
import com.intellij.util.indexing.impl.MapReduceIndex;
import com.intellij.util.indexing.impl.UpdateData;
import com.intellij.util.io.EnumeratorIntegerDescriptor;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class StubHashBasedIndexGenerator extends HashBasedIndexGenerator<Integer, SerializedStubTree> {
  private final Map<StubIndexKey, HashBasedIndexGenerator> myStubIndexesGeneratorMap = new HashMap<>();
  private final Set<StubIndexKey> myUsedKeys = new HashSet<>();

  public StubHashBasedIndexGenerator(@NotNull File out) {
    super(EnumeratorIntegerDescriptor.INSTANCE, new SerializedStubTreeDataExternalizer(
            true,
            null,
            StubForwardIndexExternalizer.FileLocalStubForwardIndexExternalizer.INSTANCE), getExtension(), out);
    for (StubIndexExtension<?, ?> stubIndexExtension : StubIndexExtension.EP_NAME.getExtensionList()) {
      FileBasedIndexExtension<?, Void> ex = StubIndexImpl
              .wrapStubIndexExtension(stubIndexExtension);
      myStubIndexesGeneratorMap.put(stubIndexExtension.getKey(), new HashBasedIndexGenerator(ex.getKeyDescriptor(),
              ex.getValueExternalizer(),
              ex,
              new File(out, getStubsDir())
      ) {

      });
    }
  }

  @NotNull
  private static String getStubsDir() {
    return StringUtil.toLowerCase(StubUpdatingIndex.INDEX_ID.getName());
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
      myUsedKeys.add(key);
      MapReduceIndex index = (MapReduceIndex)myStubIndexesGeneratorMap.get(key).getIndex();
      Map<Object, Object> reducedValue = Maps.asMap(value.keySet(), k -> null);
      index.updateWithMap(new UpdateData(hashId, reducedValue, () -> new EmptyInputDataDiffBuilder(hashId), index.getExtension().getName(), null));
    }
  }

  @Override
  public void openIndex() throws IOException {
    super.openIndex();
    for (HashBasedIndexGenerator generator : myStubIndexesGeneratorMap.values()) {
      generator.openIndex();
    }
  }

  @Override
  public void closeIndex() throws IOException {
    super.closeIndex();
    for (Map.Entry<StubIndexKey, HashBasedIndexGenerator> entry : myStubIndexesGeneratorMap.entrySet()) {
      HashBasedIndexGenerator generator = entry.getValue();
      generator.closeIndex();
    }
    ((SerializationManagerEx)SerializationManager.getInstance()).flushNameStorage();
    FileUtil.copyDir(PathManager.getIndexRoot(), new File(getOut(), getStubsDir()), f -> f.getName().startsWith("rep.names"));
  }

  private static StubUpdatingIndex getExtension() {
    return (new StubUpdatingIndex(StubForwardIndexExternalizer.FileLocalStubForwardIndexExternalizer.INSTANCE));
  }
}
