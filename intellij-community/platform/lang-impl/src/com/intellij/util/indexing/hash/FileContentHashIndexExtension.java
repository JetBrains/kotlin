// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.hash;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.ShutDownTracker;
import com.intellij.openapi.vfs.newvfs.persistent.ContentHashesUtil;
import com.intellij.util.indexing.*;
import com.intellij.util.indexing.impl.IndexStorage;
import com.intellij.util.io.*;
import org.jetbrains.annotations.NotNull;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

public class FileContentHashIndexExtension extends FileBasedIndexExtension<Integer, Void> implements CustomImplementationFileBasedIndexExtension<Integer, Void>, CustomInputsIndexFileBasedIndexExtension<Integer>, Disposable {
  private static final Logger LOG = Logger.getInstance(FileContentHashIndexExtension.class);
  public static final ID<Integer, Void> HASH_INDEX_ID = ID.create("file.content.hash.index");

  @NotNull
  private final ContentHashesUtil.HashEnumerator myEnumerator;
  private final int myDirHash;

  @NotNull
  public static FileContentHashIndexExtension create(@NotNull File enumeratorDir, @NotNull Disposable parent) throws IOException {
    FileContentHashIndexExtension extension = new FileContentHashIndexExtension(enumeratorDir);
    Disposer.register(parent, extension);
    return extension;
  }

  private FileContentHashIndexExtension(@NotNull File enumeratorDir) throws IOException {
    myEnumerator = new ContentHashesUtil.HashEnumerator(enumeratorDir);
    myDirHash = enumeratorDir.getAbsolutePath().hashCode();
    ShutDownTracker.getInstance().registerShutdownTask(() -> closeEnumerator());
  }

  @NotNull
  @Override
  public ID<Integer, Void> getName() {
    return HASH_INDEX_ID;
  }

  @NotNull
  @Override
  public FileBasedIndex.InputFilter getInputFilter() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean dependsOnFileContent() {
    return true;
  }

  @NotNull
  @Override
  public DataIndexer<Integer, Void, FileContent> getIndexer() {
    return fc -> {
      byte[] hash = ((FileContentImpl)fc).getHash();
      LOG.assertTrue(hash != null);
      try {
        int id;
        synchronized (myEnumerator) {
          id = myEnumerator.tryEnumerate(hash);
        }
        return id == 0 ? Collections.emptyMap() : Collections.singletonMap(id, null);
      }
      catch (IOException e) {
        throw new RuntimeException(e);
      }
    };
  }

  @NotNull
  @Override
  public KeyDescriptor<Integer> getKeyDescriptor() {
    return EnumeratorIntegerDescriptor.INSTANCE;
  }

  @NotNull
  @Override
  public DataExternalizer<Void> getValueExternalizer() {
    return VoidDataExternalizer.INSTANCE;
  }

  @Override
  public int getVersion() {
    return myDirHash;
  }

  @Override
  public void dispose() {
    closeEnumerator();
  }

  @NotNull
  @Override
  public DataExternalizer<Collection<Integer>> createExternalizer() {
    return new DataExternalizer<Collection<Integer>>() {
      @Override
      public void save(@NotNull DataOutput out, Collection<Integer> value) throws IOException {
        assert value.isEmpty() || value.size() == 1;
        DataInputOutputUtil.writeINT(out, value.isEmpty() ? 0 : value.iterator().next());
      }

      @Override
      public Collection<Integer> read(@NotNull DataInput in) throws IOException {
        int id = DataInputOutputUtil.readINT(in);
        return id == 0 ? Collections.emptyList() : Collections.singleton(id);
      }
    };
  }

  private void closeEnumerator() {
    synchronized (myEnumerator) {
      if (myEnumerator.isClosed()) return;
      try {
        myEnumerator.close();
      }
      catch (IOException e) {
        LOG.error(e);
      }
    }
  }

  @NotNull
  @Override
  public UpdatableIndex<Integer, Void, FileContent> createIndexImplementation(@NotNull FileBasedIndexExtension<Integer, Void> extension,
                                                                              @NotNull IndexStorage<Integer, Void> storage)
    throws IOException {
    return new FileContentHashIndex(((FileContentHashIndexExtension)extension), storage);
  }
}
