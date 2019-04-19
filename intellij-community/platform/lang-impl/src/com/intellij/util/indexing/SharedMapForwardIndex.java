// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.io.ByteArraySequence;
import com.intellij.openapi.vfs.newvfs.persistent.PersistentFS;
import com.intellij.util.indexing.impl.forward.AbstractForwardIndexAccessor;
import com.intellij.util.indexing.impl.forward.ForwardIndex;
import com.intellij.util.indexing.impl.forward.PersistentMapBasedForwardIndex;
import com.intellij.util.io.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class SharedMapForwardIndex implements ForwardIndex {
  private static final Logger LOG = Logger.getInstance(SharedMapForwardIndex.class);
  private final ID<?, ?> myIndexId;

  // only verification purpose
  @Nullable
  private final PersistentMapBasedForwardIndex mySanityVerificationIndex;

  // only verification purpose
  @Nullable
  private final AbstractForwardIndexAccessor<?, ?, ?, ?> myAccessor;

  public SharedMapForwardIndex(@NotNull IndexExtension<?, ?, ?> extension,
                               @Nullable AbstractForwardIndexAccessor<?, ?, ?, ?> accessor,
                               @Nullable File verificationIndexStorageFile,
                               boolean verificationIndexHasChunks) throws IOException {
    myIndexId = (ID<?, ?>)extension.getName();
    if (verificationIndexStorageFile != null && (!SharedIndicesData.ourFileSharedIndicesEnabled || SharedIndicesData.DO_CHECKS)) {
      Boolean old = PersistentHashMapValueStorage.CreationTimeOptions.HAS_NO_CHUNKS.get();
      try {
        PersistentHashMapValueStorage.CreationTimeOptions.HAS_NO_CHUNKS.set(!verificationIndexHasChunks);
        mySanityVerificationIndex = new PersistentMapBasedForwardIndex(verificationIndexStorageFile);
      }
      catch (IOException e) {
        IOUtil.deleteAllFilesStartingWith(verificationIndexStorageFile);
        throw e;
      }
      finally {
        PersistentHashMapValueStorage.CreationTimeOptions.HAS_NO_CHUNKS.set(old);
      }
    }
    else {
      mySanityVerificationIndex = null;
    }
    myAccessor = accessor;
  }

  @Nullable
  @Override
  public ByteArraySequence get(@NotNull Integer inputId) throws IOException {
    if (!SharedIndicesData.ourFileSharedIndicesEnabled) {
      assert mySanityVerificationIndex != null;
      return mySanityVerificationIndex.get(inputId);
    }
    ByteArraySequence data = SharedIndicesData.recallFileData(inputId, myIndexId, ByteSequenceDataExternalizer.INSTANCE);
    if (mySanityVerificationIndex != null) {
      ByteArraySequence verificationData = mySanityVerificationIndex.get(inputId);
      if (!Comparing.equal(verificationData, data)) {
        if (verificationData != null) {
          SharedIndicesData.associateFileData(inputId, myIndexId, verificationData, ByteSequenceDataExternalizer.INSTANCE);
          if (data != null) {
            LOG.error("Unexpected indexing diff with hash id " +
                      myIndexId +
                      ", file:" +
                      IndexInfrastructure.findFileById(PersistentFS.getInstance(), inputId)
                      +
                      "," +
                      deserializeOrRawString(verificationData) +
                      "," +
                      deserializeOrRawString(data));
          }
        }
        data = verificationData;
      }
    }
    return data;
  }

  @Override
  public void put(@NotNull Integer inputId, @Nullable ByteArraySequence value) throws IOException {
    if (SharedIndicesData.ourFileSharedIndicesEnabled) {
      SharedIndicesData.associateFileData(inputId, myIndexId, value, ByteSequenceDataExternalizer.INSTANCE);
    }
    if (mySanityVerificationIndex != null) {
      mySanityVerificationIndex.put(inputId, value);
    }
  }

  @Override
  public void force() {
    if (mySanityVerificationIndex != null) mySanityVerificationIndex.force();
  }

  @Override
  public void clear() throws IOException {
    if (mySanityVerificationIndex != null) mySanityVerificationIndex.clear();
  }

  @Override
  public void close() throws IOException {
    if (mySanityVerificationIndex != null) mySanityVerificationIndex.close();
  }

  private Object deserializeOrRawString(ByteArraySequence seq) throws IOException {
    if (myAccessor != null) return myAccessor.deserializeData(seq);
    if (seq == null) return null;
    return Arrays.toString(seq.getBytes());
  }
}
