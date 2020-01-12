// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.hash;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.ShutDownTracker;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.hash.ContentHashEnumerator;
import com.intellij.util.indexing.*;
import com.intellij.util.indexing.impl.IndexStorage;
import com.intellij.util.indexing.snapshot.IndexedHashesSupport;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.DataInputOutputUtil;
import com.intellij.util.io.KeyDescriptor;
import com.intellij.util.io.VoidDataExternalizer;
import org.jetbrains.annotations.NotNull;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;

public class FileContentHashIndexExtension extends FileBasedIndexExtension<Long, Void> implements CustomImplementationFileBasedIndexExtension<Long, Void>, CustomInputsIndexFileBasedIndexExtension<Long>, Disposable {
  private static final Logger LOG = Logger.getInstance(FileContentHashIndexExtension.class);
  public static final ID<Long, Void> HASH_INDEX_ID = ID.create("file.content.hash.index");

  private final ContentHashEnumerator @NotNull [] myEnumerators;

  @NotNull
  public static FileContentHashIndexExtension create(Path @NotNull [] enumeratorDirs, @NotNull Disposable parent) throws IOException {
    FileContentHashIndexExtension extension = new FileContentHashIndexExtension(enumeratorDirs);
    RebuildStatus.registerIndex(extension.getName());
    Disposer.register(parent, extension);
    return extension;
  }

  private FileContentHashIndexExtension(Path @NotNull [] enumeratorDirs) throws IOException {
    IOException[] exception = {null};
    myEnumerators = ContainerUtil.map2Array(enumeratorDirs, ContentHashEnumerator.class, d -> {
      try {
        return new ContentHashEnumerator(d.getParent().resolve("hashes"));
      }
      catch (IOException e) {
        exception[0] = e;
        return null;
      }
    });
    if (exception[0] != null) {
      throw exception[0];
    }
    ShutDownTracker.getInstance().registerShutdownTask(() -> closeEnumerator());
  }

  @NotNull
  @Override
  public ID<Long, Void> getName() {
    return HASH_INDEX_ID;
  }

  @NotNull
  @Override
  public FileBasedIndex.InputFilter getInputFilter() {
    return file -> !file.isDirectory();
  }

  @Override
  public boolean dependsOnFileContent() {
    return true;
  }

  @NotNull
  @Override
  public DataIndexer<Long, Void, FileContent> getIndexer() {
    return fc -> {
      long hashId = getHashId(fc);
      if (hashId != NULL_HASH_ID) return Collections.singletonMap(hashId, null);
      byte[] hash = IndexedHashesSupport.getOrInitIndexedHash((FileContentImpl) fc, false);
      try {
        hashId = tryEnumerate(hash);
        setHashId(fc, hashId);
        return hashId == NULL_HASH_ID ? Collections.emptyMap() : Collections.singletonMap(hashId, null);
      }
      catch (IOException e) {
        throw new RuntimeException(e);
      }
    };
  }

  private Long tryEnumerate(byte[] hash) throws IOException {
    for (int i = 0; i < myEnumerators.length; i++) {
      ContentHashEnumerator enumerator = myEnumerators[i];
      //noinspection SynchronizationOnLocalVariableOrMethodParameter
      synchronized (enumerator) {
        int id = Math.abs(enumerator.tryEnumerate(hash));
        if (id != 0) {
          return getHashId(id, i);
        }
      }
    }
    return NULL_HASH_ID;
  }

  @NotNull
  @Override
  public KeyDescriptor<Long> getKeyDescriptor() {
    return new KeyDescriptor<Long>() {
      @Override
      public int getHashCode(Long value) {
        return value.hashCode();
      }

      @Override
      public boolean isEqual(Long val1, Long val2) {
        return val1.longValue() == val2.longValue();
      }

      @Override
      public void save(@NotNull DataOutput out, Long value) throws IOException {
        DataInputOutputUtil.writeLONG(out, value);
      }

      @Override
      public Long read(@NotNull DataInput in) throws IOException {
        return DataInputOutputUtil.readLONG(in);
      }
    };
  }

  @NotNull
  @Override
  public DataExternalizer<Void> getValueExternalizer() {
    return VoidDataExternalizer.INSTANCE;
  }

  @Override
  public int getVersion() {
    return 0;
  }

  @Override
  public void dispose() {
    closeEnumerator();
  }

  @NotNull
  @Override
  public DataExternalizer<Collection<Long>> createExternalizer() {
    return new DataExternalizer<Collection<Long>>() {
      @Override
      public void save(@NotNull DataOutput out, Collection<Long> value) throws IOException {
        assert value.isEmpty() || value.size() == 1;
        DataInputOutputUtil.writeLONG(out, value.isEmpty() ? 0 : value.iterator().next());
      }

      @Override
      public Collection<Long> read(@NotNull DataInput in) throws IOException {
        long id = DataInputOutputUtil.readLONG(in);
        return id == 0 ? Collections.emptyList() : Collections.singleton(id);
      }
    };
  }

  private void closeEnumerator() {
    for (ContentHashEnumerator enumerator : myEnumerators) {
      synchronized (enumerator) {
        if (enumerator.isClosed()) return;
        try {
          enumerator.close();
        }
        catch (IOException e) {
          LOG.error(e);
        }
      }
    }
  }

  @NotNull
  @Override
  public UpdatableIndex<Long, Void, FileContent> createIndexImplementation(@NotNull FileBasedIndexExtension<Long, Void> extension,
                                                                           @NotNull IndexStorage<Long, Void> storage)
    throws IOException {
    return new FileContentHashIndex(((FileContentHashIndexExtension)extension), storage);
  }

  static int getIndexId(long hashId) {
    return (int)(hashId >> 32);
  }

  static int getInternalHashId(long hashId) {
    return (int)hashId;
  }

  static long getHashId(int internalHashId, int indexId) {
    return (((long) indexId) << 32) | (internalHashId & 0xffffffffL);
  }

  public static final long NULL_HASH_ID = getHashId(0, -1);

  public static final Key<Long> HASH_ID_KEY = Key.create("file.content.hash.id");

  public static long getHashId(@NotNull FileContent content) {
    Long value = HASH_ID_KEY.get(content);
    return value == null ? NULL_HASH_ID : value;
  }

  public static void setHashId(@NotNull FileContent content, long hashId) {
    HASH_ID_KEY.set(content, hashId);
  }
}
