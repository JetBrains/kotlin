// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.impl.forward;

import com.intellij.util.io.EnumeratorIntegerDescriptor;
import com.intellij.util.io.IOUtil;
import com.intellij.util.io.PersistentHashMap;
import com.intellij.util.io.PersistentHashMapValueStorage;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;

public class IntMapForwardIndex implements IntForwardIndex {
  private final File myStorageFile;
  private final boolean myHasChunks;

  private volatile PersistentHashMap<Integer, Integer> myPersistentMap;

  public IntMapForwardIndex(@NotNull File storageFile,
                            boolean hasChunks) throws IOException {
    myStorageFile = storageFile;
    myHasChunks = hasChunks;
    createMap();
  }

  private void createMap() throws IOException {
    Boolean old = PersistentHashMapValueStorage.CreationTimeOptions.HAS_NO_CHUNKS.get();
    try {
      PersistentHashMapValueStorage.CreationTimeOptions.HAS_NO_CHUNKS.set(!myHasChunks);
      myPersistentMap = new PersistentHashMap<Integer, Integer>(myStorageFile.toPath(), EnumeratorIntegerDescriptor.INSTANCE, EnumeratorIntegerDescriptor.INSTANCE) {
        @Override
        protected boolean wantNonNegativeIntegralValues() {
          return true;
        }
      };
    }
    catch (IOException e) {
      IOUtil.deleteAllFilesStartingWith(myStorageFile);
      throw e;
    }
    finally {
      PersistentHashMapValueStorage.CreationTimeOptions.HAS_NO_CHUNKS.set(old);
    }
  }


  @Override
  public int getInt(@NotNull Integer key) throws IOException {
    assert myPersistentMap != null;
    return myPersistentMap.get(key);
  }

  @Override
  public void putInt(@NotNull Integer key, int value) throws IOException {
    assert myPersistentMap != null;
    myPersistentMap.put(key, value);
  }

  @Override
  public void force() {
    if (myPersistentMap != null) myPersistentMap.force();
  }

  @Override
  public void clear() throws IOException {
    PersistentHashMap.deleteMap(myPersistentMap);
    createMap();
  }

  @Override
  public void close() throws IOException {
    if (myPersistentMap != null) myPersistentMap.close();
  }
}
