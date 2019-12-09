// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.impl.perFileVersion;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.util.io.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.function.Supplier;

public class PersistentSubIndexerVersionEnumerator<SubIndexerVersion> implements Closeable {
  private static final Logger LOG = Logger.getInstance(PersistentSubIndexerVersionEnumerator.class);
  private static volatile int COMPACT_THRESHOLD = 500_000;

  private class MyEnumerator implements DataEnumerator<SubIndexerVersion> {
    @Override
    public synchronized int enumerate(@Nullable SubIndexerVersion value) throws IOException {
      Integer val = myMap.get(value);
      if (val != null){ return val;}
      myMap.put(value, ++myNextVersion);
      if (myNextVersion == Integer.MAX_VALUE) {
        throw new IOException("Request index rebuild");
      }
      return myNextVersion;
    }

    @Nullable
    @Override
    public SubIndexerVersion valueOf(int idx) {
      throw new UnsupportedOperationException();
    }
  }

  @NotNull
  private final CachingEnumerator<SubIndexerVersion> myEnumerator;
  @NotNull
  private final File myFile;
  @NotNull
  private final KeyDescriptor<SubIndexerVersion> mySubIndexerTypeDescriptor;
  private volatile PersistentHashMap<SubIndexerVersion, Integer> myMap;
  private volatile int myNextVersion;

  public PersistentSubIndexerVersionEnumerator(@NotNull File file,
                                               @NotNull KeyDescriptor<SubIndexerVersion> subIndexerTypeDescriptor,
                                               @NotNull Supplier<? extends Collection<SubIndexerVersion>> allSubIndexerVersions) throws IOException {
    myFile = file;
    mySubIndexerTypeDescriptor = subIndexerTypeDescriptor;
    myEnumerator = new CachingEnumerator<>(new MyEnumerator(), subIndexerTypeDescriptor);
    init(allSubIndexerVersions);
    if (myNextVersion >= Integer.MAX_VALUE - COMPACT_THRESHOLD) {
      throw new IOException("Rebuild index due to attribute version enumerator overflow");
    }
  }

  public int enumerate(SubIndexerVersion version) throws IOException {
    return myEnumerator.enumerate(version);
  }

  private void removeStaleKeys(@Nullable Supplier<? extends Collection<SubIndexerVersion>> allSubIndexerSupplier) throws IOException {
    if (myMap.getSize() > COMPACT_THRESHOLD) {
      if (allSubIndexerSupplier == null) {
        throw new IOException("max sub indexer limit is exceeded");
      }
      Collection<SubIndexerVersion> subIndexerVersions = allSubIndexerSupplier.get();
      for (SubIndexerVersion attr : myMap.getAllKeysWithExistingMapping()) {
        if (!subIndexerVersions.contains(attr)) {
          myMap.remove(attr);
          // next time indexer is required we reindex file - it's ok
        }
      }
    }
  }

  private void init(@Nullable Supplier<? extends Collection<SubIndexerVersion>> allSubIndexerVersions) throws IOException {
    myMap = new PersistentHashMap<SubIndexerVersion, Integer>(myFile, mySubIndexerTypeDescriptor, EnumeratorIntegerDescriptor.INSTANCE) {
      //@Override
      //protected boolean wantNonNegativeIntegralValues() {
      //  return true;
      //}
      // getSize/remove are required here
    };
    File nextVersionFile = getNextVersionFile(myFile);
    String intValue = nextVersionFile.exists() ? FileUtil.loadFile(nextVersionFile, StandardCharsets.UTF_8) : String.valueOf(1);
    try {
      myNextVersion = Integer.parseInt(intValue);
    }
    catch (NumberFormatException e) {
      throw new IOException("Invalid next version format " + intValue);
    }
    removeStaleKeys(allSubIndexerVersions);
  }

  public void clear() throws IOException {
    try {
      close();
    } catch (IOException e) {
      LOG.error(e);
    } finally {
      PersistentHashMap.deleteFilesStartingWith(myFile);
      init( null);
    }
  }

  public void flush() throws IOException {
    myMap.force();
    FileUtil.writeToFile(getNextVersionFile(myFile), String.valueOf(myNextVersion));
  }

  @Override
  public void close() throws IOException {
    if (!myMap.isClosed()) {
      myMap.close();
    }
    FileUtil.writeToFile(getNextVersionFile(myFile), String.valueOf(myNextVersion));
  }

  @NotNull
  private static File getNextVersionFile(File baseFile) {
    return new File(baseFile.getAbsolutePath() + ".next");
  }

  @TestOnly
  public static void setCompactThreshold(int compactThreshold) {
    COMPACT_THRESHOLD = compactThreshold;
  }

  @TestOnly
  public static int getCompactThreshold() {
    return COMPACT_THRESHOLD;
  }
}
