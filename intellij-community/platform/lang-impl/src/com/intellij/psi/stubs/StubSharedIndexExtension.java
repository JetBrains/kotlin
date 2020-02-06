// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.stubs;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Disposer;
import com.intellij.util.indexing.ID;
import com.intellij.util.indexing.provided.SharedIndexExtension;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.EnumeratorIntegerDescriptor;
import com.intellij.util.io.KeyDescriptor;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

public class StubSharedIndexExtension implements SharedIndexExtension<Integer, SerializedStubTree> {
  @NotNull
  @Override
  public KeyDescriptor<Integer> createKeyDescriptor(@NotNull Path indexPath) {
    return EnumeratorIntegerDescriptor.INSTANCE;
  }

  @NotNull
  @Override
  public DataExternalizer<SerializedStubTree> createValueExternalizer(@NotNull Path indexPath) {
    SerializationManagerImpl manager = new SerializationManagerImpl(getStubSerializerNamesStorageFile(indexPath.getParent()), true);
    Disposer.register(ApplicationManager.getApplication(), manager);
    return new SerializedStubTreeDataExternalizer(true, manager, StubForwardIndexExternalizer.createFileLocalExternalizer(manager));
  }

  @NotNull
  public static Path getStubSerializerNamesStorageFile(@NotNull Path stubIndexesRoot) {
    return stubIndexesRoot.resolve("serializerNames").resolve("names");
  }
}
