// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.stubs.provided;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.stubs.*;
import com.intellij.util.indexing.ID;
import com.intellij.util.indexing.provided.ProvidedIndexExtension;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.EnumeratorIntegerDescriptor;
import com.intellij.util.io.KeyDescriptor;
import com.intellij.util.io.VoidDataExternalizer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;

public class StubProvidedIndexExtension implements ProvidedIndexExtension<Integer, SerializedStubTree> {
  @NotNull
  private final Path myIndexFile;

  public StubProvidedIndexExtension(@NotNull Path file) {myIndexFile = file;}

  @NotNull
  @Override
  public Path getIndexPath() {
    return myIndexFile;
  }

  @NotNull
  @Override
  public ID<Integer, SerializedStubTree> getIndexId() {
    return StubUpdatingIndex.INDEX_ID;
  }

  @NotNull
  @Override
  public KeyDescriptor<Integer> createKeyDescriptor() {
    return EnumeratorIntegerDescriptor.INSTANCE;
  }

  @NotNull
  @Override
  public DataExternalizer<SerializedStubTree> createValueExternalizer() {
    Path path = getIndexPath();
    SerializationManagerImpl manager =
      new SerializationManagerImpl(path.resolve(StringUtil.toLowerCase(StubUpdatingIndex.INDEX_ID.getName())).resolve("rep.names"),
                                   true);
    Disposer.register(ApplicationManager.getApplication(), manager);
    return new SerializedStubTreeDataExternalizer(false, manager, StubForwardIndexExternalizer.FileLocalStubForwardIndexExternalizer.INSTANCE);
  }

  @Nullable
  public <K> ProvidedIndexExtension<K, Void> findProvidedStubIndex(@NotNull StubIndexExtension<K, ?> extension) {
    String name = extension.getKey().getName();
    Path path = getIndexPath();

    Path indexPath = path.resolve(name);
    if (!Files.exists(indexPath)) return null;

    return new ProvidedIndexExtension<K, Void>() {
      @NotNull
      @Override
      public Path getIndexPath() {
        return myIndexFile;
      }

      @NotNull
      @Override
      public ID<K, Void> getIndexId() {
        return (ID)extension.getKey();
      }

      @NotNull
      @Override
      public KeyDescriptor<K> createKeyDescriptor() {
        return extension.getKeyDescriptor();
      }

      @NotNull
      @Override
      public DataExternalizer<Void> createValueExternalizer() {
        return VoidDataExternalizer.INSTANCE;
      }
    };
  }
}
