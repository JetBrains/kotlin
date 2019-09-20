// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.vfs.newvfs.persistent.PersistentFS;
import com.intellij.util.indexing.impl.forward.IntForwardIndex;
import com.intellij.util.io.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;

public class SharedIntMapForwardIndex implements IntForwardIndex {
  private static final Logger LOG = Logger.getInstance(SharedIntMapForwardIndex.class);
  private final ID<?, ?> myIndexId;
  private final File myVerificationIndexStorageFile;
  private final boolean myVerificationIndexHasChunks;

  private volatile PersistentHashMap<Integer, Integer> myPersistentMap;

  public SharedIntMapForwardIndex(@NotNull IndexExtension<?, ?, ?> extension,
                                  @Nullable File verificationIndexStorageFile,
                                  boolean verificationIndexHasChunks) throws IOException {
    myIndexId = (ID<?, ?>)extension.getName();
    myVerificationIndexStorageFile = verificationIndexStorageFile;
    myVerificationIndexHasChunks = verificationIndexHasChunks;
    if (verificationIndexStorageFile != null && (!SharedIndicesData.ourFileSharedIndicesEnabled || SharedIndicesData.DO_CHECKS)) {
      createMap();
    }
  }

  private void createMap() throws IOException {
    Boolean old = PersistentHashMapValueStorage.CreationTimeOptions.HAS_NO_CHUNKS.get();
    try {
      PersistentHashMapValueStorage.CreationTimeOptions.HAS_NO_CHUNKS.set(!myVerificationIndexHasChunks);
      myPersistentMap = new PersistentHashMap<Integer, Integer>(myVerificationIndexStorageFile, EnumeratorIntegerDescriptor.INSTANCE, EnumeratorIntegerDescriptor.INSTANCE) {
        @Override
        protected boolean wantNonNegativeIntegralValues() {
          return true;
        }
      };
    }
    catch (IOException e) {
      IOUtil.deleteAllFilesStartingWith(myVerificationIndexStorageFile);
      throw e;
    }
    finally {
      PersistentHashMapValueStorage.CreationTimeOptions.HAS_NO_CHUNKS.set(old);
    }
  }


  @Override
  public int getInt(@NotNull Integer key) throws IOException {
    if (!SharedIndicesData.ourFileSharedIndicesEnabled) {
      assert myPersistentMap != null;
      return myPersistentMap.get(key);
    }
    Integer data = SharedIndicesData.recallFileData(key, myIndexId, EnumeratorIntegerDescriptor.INSTANCE);
    if (myPersistentMap != null) {
      Integer verificationValue = myPersistentMap.get(key);
      if (!Comparing.equal(verificationValue, data)) {
        if (verificationValue != null) {
          SharedIndicesData.associateFileData(key, myIndexId, verificationValue, EnumeratorIntegerDescriptor.INSTANCE);
          if (data != null) {
            LOG.error("Unexpected indexing diff with hash id " +
                      myIndexId +
                      ", file:" +
                      IndexInfrastructure.findFileById(PersistentFS.getInstance(), key)
                      +
                      "," +
                      verificationValue +
                      "," +
                      data);
          }
        }
        data = verificationValue;
      }
    }
    return data == null ? 0 : data.intValue();
  }

  @Override
  public void putInt(@NotNull Integer key, int value) throws IOException {
    if (SharedIndicesData.ourFileSharedIndicesEnabled) {
      SharedIndicesData.associateFileData(key, myIndexId, value, EnumeratorIntegerDescriptor.INSTANCE);
    }
    if (myPersistentMap != null) {
      myPersistentMap.put(key, value);
    }
  }

  @Override
  public void force() {
    if (myPersistentMap != null) myPersistentMap.force();
  }

  @Override
  public void clear() throws IOException {
    File baseFile = myPersistentMap.getBaseFile();
    try {
      myPersistentMap.close();
    }
    catch (IOException e) {
      LOG.info(e);
    }
    PersistentHashMap.deleteFilesStartingWith(baseFile);
    createMap();
  }

  @Override
  public void close() throws IOException {
    if (myPersistentMap != null) myPersistentMap.close();
  }
}
