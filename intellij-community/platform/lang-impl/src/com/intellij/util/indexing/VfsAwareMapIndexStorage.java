/*
 * Copyright 2000-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intellij.util.indexing;

import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.ProjectAndLibrariesScope;
import com.intellij.psi.search.ProjectScopeImpl;
import com.intellij.util.Processor;
import com.intellij.util.SystemProperties;
import com.intellij.util.ThrowableRunnable;
import com.intellij.util.containers.ConcurrentIntObjectMap;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.indexing.impl.MapIndexStorage;
import com.intellij.util.indexing.impl.UpdatableValueContainer;
import com.intellij.util.io.DataOutputStream;
import com.intellij.util.io.*;
import gnu.trove.TIntHashSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.io.*;

/**
 * @author Eugene Zhuravlev
 */
public final class VfsAwareMapIndexStorage<Key, Value> extends MapIndexStorage<Key, Value> implements VfsAwareIndexStorage<Key, Value> {
  private static final Logger LOG = Logger.getInstance(MapIndexStorage.class);
  private static final boolean ENABLE_CACHED_HASH_IDS = SystemProperties.getBooleanProperty("idea.index.no.cashed.hashids", true);
  private final boolean myBuildKeyHashToVirtualFileMapping;
  private AppendableStorageBackedByResizableMappedFile myKeyHashToVirtualFileMapping;
  private volatile int myLastScannedId;

  private static final ConcurrentIntObjectMap<Boolean> ourInvalidatedSessionIds = ContainerUtil.createConcurrentIntObjectMap();

  @TestOnly
  public VfsAwareMapIndexStorage(@NotNull File storageFile,
                                 @NotNull KeyDescriptor<Key> keyDescriptor,
                                 @NotNull DataExternalizer<Value> valueExternalizer,
                                 final int cacheSize,
                                 final boolean readOnly
  ) throws IOException {
    super(storageFile, keyDescriptor, valueExternalizer, cacheSize, false, true, readOnly, null);
    myBuildKeyHashToVirtualFileMapping = false;
  }

  public VfsAwareMapIndexStorage(@NotNull File storageFile,
                                 @NotNull KeyDescriptor<Key> keyDescriptor,
                                 @NotNull DataExternalizer<Value> valueExternalizer,
                                 final int cacheSize,
                                 boolean keyIsUniqueForIndexedFile,
                                 boolean buildKeyHashToVirtualFileMapping) throws IOException {
    super(storageFile, keyDescriptor, valueExternalizer, cacheSize, keyIsUniqueForIndexedFile, false, false, null);
    myBuildKeyHashToVirtualFileMapping = buildKeyHashToVirtualFileMapping;
    initMapAndCache();
  }

  @Override
  protected void initMapAndCache() throws IOException {
    super.initMapAndCache();
    myKeyHashToVirtualFileMapping = myBuildKeyHashToVirtualFileMapping ?
                                    new AppendableStorageBackedByResizableMappedFile(getProjectFile(), 4096, null, PagedFileStorage.MB, true) : null;
  }

  @Override
  protected void checkCanceled() {
    ProgressManager.checkCanceled();
  }

  @NotNull
  private File getProjectFile() {
    return new File(myBaseStorageFile.getPath() + ".project");
  }

  private <T extends Throwable> void withLock(ThrowableRunnable<T> r) throws T {
    myKeyHashToVirtualFileMapping.getPagedFileStorage().lock();
    try {
      r.run();
    } finally {
      myKeyHashToVirtualFileMapping.getPagedFileStorage().unlock();
    }
  }

  @Override
  public void flush() {
    l.lock();
    try {
      super.flush();
      if (myKeyHashToVirtualFileMapping != null && myKeyHashToVirtualFileMapping.isDirty()) {
        withLock(() -> myKeyHashToVirtualFileMapping.force());
      }
    }
    finally {
      l.unlock();
    }
  }

  @Override
  public void close() throws StorageException {
    super.close();
    try {
      if (myKeyHashToVirtualFileMapping != null) {
        withLock(() -> myKeyHashToVirtualFileMapping.close());
      }
    }
    catch (RuntimeException e) {
      unwrapCauseAndRethrow(e);
    }
  }

  @Override
  public void clear() throws StorageException{
    try {
      if (myKeyHashToVirtualFileMapping != null) {
        withLock(() -> myKeyHashToVirtualFileMapping.close());
      }
    }
    catch (RuntimeException e) {
      LOG.info(e);
    }
    try {
      if (myKeyHashToVirtualFileMapping != null) IOUtil.deleteAllFilesStartingWith(getProjectFile());
    }
    catch (RuntimeException e) {
      unwrapCauseAndRethrow(e);
    }
    super.clear();
  }

  @Override
  public boolean processKeys(@NotNull Processor<? super Key> processor, GlobalSearchScope scope, final IdFilter idFilter) throws StorageException {
    l.lock();
    try {
      myCache.clear(); // this will ensure that all new keys are made into the map

      if (myBuildKeyHashToVirtualFileMapping && idFilter != null) {
        TIntHashSet hashMaskSet = null;
        long l = System.currentTimeMillis();
        GlobalSearchScope filterScope = idFilter.getEffectiveFilteringScope();
        GlobalSearchScope effectiveFilteringScope = filterScope != null ? filterScope : scope;

        File fileWithCaches = getSavedProjectFileValueIds(myLastScannedId, effectiveFilteringScope);
        final boolean useCachedHashIds = ENABLE_CACHED_HASH_IDS &&
                                         (effectiveFilteringScope instanceof ProjectScopeImpl || effectiveFilteringScope instanceof ProjectAndLibrariesScope) &&
                                         fileWithCaches != null;
        int id = myKeyHashToVirtualFileMapping.getCurrentLength();

        if (useCachedHashIds && id == myLastScannedId) {
          if (ourInvalidatedSessionIds.remove(id) == null) {
            try {
              hashMaskSet = loadHashedIds(fileWithCaches);
            }
            catch (IOException ignored) {
            }
          }
        }

        if (hashMaskSet == null) {
          if (useCachedHashIds && myLastScannedId != 0) {
            FileUtil.asyncDelete(fileWithCaches);
          }

          hashMaskSet = new TIntHashSet(1000);
          final TIntHashSet finalHashMaskSet = hashMaskSet;
          withLock(() -> {
            myKeyHashToVirtualFileMapping.force();
            ProgressManager.checkCanceled();

            myKeyHashToVirtualFileMapping.processAll(key -> {
              ProgressManager.checkCanceled();
              if (!idFilter.containsFileId(key[1])) return true;
              finalHashMaskSet.add(key[0]);
              return true;
            }, IntPairInArrayKeyDescriptor.INSTANCE);
          });

          if (useCachedHashIds) {
            saveHashedIds(hashMaskSet, id, effectiveFilteringScope);
          }
        }

        if (LOG.isDebugEnabled()) {
          LOG.debug("Scanned keyHashToVirtualFileMapping of " + myBaseStorageFile + " for " + (System.currentTimeMillis() - l));
        }
        final TIntHashSet finalHashMaskSet = hashMaskSet;
        return doProcessKeys(key -> {
          if (!finalHashMaskSet.contains(myKeyDescriptor.getHashCode(key))) return true;
          return processor.process(key);
        });
      }
      return doProcessKeys(processor);
    }
    catch (IOException e) {
      throw new StorageException(e);
    }
    catch (RuntimeException e) {
      return unwrapCauseAndRethrow(e);
    }
    finally {
      l.unlock();
    }
  }

  private boolean doProcessKeys(@NotNull Processor<? super Key> processor) throws IOException {
    return myMap instanceof PersistentHashMap && PersistentEnumeratorBase.inlineKeyStorage(myKeyDescriptor)
           // process keys and check that they're already present in map because we don't have separated key storage we must check keys
           ? ((PersistentHashMap<Key, UpdatableValueContainer<Value>>)myMap).processKeysWithExistingMapping(processor)
           // optimization: process all keys, some of them might be already deleted but we don't care. We just read key storage file here
           : myMap.processKeys(processor);
  }

  @NotNull
  private static TIntHashSet loadHashedIds(@NotNull File fileWithCaches) throws IOException {
    try (DataInputStream inputStream = new DataInputStream(new BufferedInputStream(new FileInputStream(fileWithCaches)))) {
      int capacity = DataInputOutputUtil.readINT(inputStream);
      TIntHashSet hashMaskSet = new TIntHashSet(capacity);
      while (capacity > 0) {
        hashMaskSet.add(DataInputOutputUtil.readINT(inputStream));
        --capacity;
      }
      return hashMaskSet;
    }
  }

  private void saveHashedIds(@NotNull TIntHashSet hashMaskSet, int largestId, @NotNull GlobalSearchScope scope) {
    File newFileWithCaches = getSavedProjectFileValueIds(largestId, scope);
    assert newFileWithCaches != null;

    boolean savedSuccessfully;
    try (DataOutputStream stream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(newFileWithCaches)))) {
      DataInputOutputUtil.writeINT(stream, hashMaskSet.size());
      savedSuccessfully = hashMaskSet.forEach(value -> {
        try {
          DataInputOutputUtil.writeINT(stream, value);
          return true;
        }
        catch (IOException ex) {
          return false;
        }
      });
    }
    catch (IOException ignored) {
      savedSuccessfully = false;
    }
    if (savedSuccessfully) {
      myLastScannedId = largestId;
    }
  }

  private static volatile File mySessionDirectory;
  private static File getSessionDir() {
    File sessionDirectory = mySessionDirectory;
    if (sessionDirectory == null) {
      synchronized (VfsAwareMapIndexStorage.class) {
        sessionDirectory = mySessionDirectory;
        if (sessionDirectory == null) {
          try {
            mySessionDirectory = sessionDirectory = FileUtil.createTempDirectory(new File(PathManager.getTempPath()), Long.toString(System.currentTimeMillis()), "", true);
          } catch (IOException ex) {
            throw new RuntimeException("Can not create temp directory", ex);
          }
        }
      }
    }
    return sessionDirectory;
  }

  @Nullable
  private File getSavedProjectFileValueIds(int id, @NotNull GlobalSearchScope scope) {
    Project project = scope.getProject();
    if (project == null) return null;
    return new File(getSessionDir(), getProjectFile().getName() + "." + project.hashCode() + "." + id + "." + scope.isSearchInLibraries());
  }

  @Override
  public void addValue(final Key key, final int inputId, final Value value) throws StorageException {
    try {
      if (myKeyHashToVirtualFileMapping != null) {
        withLock(() -> myKeyHashToVirtualFileMapping.append(new int[] { myKeyDescriptor.getHashCode(key), inputId }, IntPairInArrayKeyDescriptor.INSTANCE));
        int lastScannedId = myLastScannedId;
        if (lastScannedId != 0) { // we have write lock
          ourInvalidatedSessionIds.cacheOrGet(lastScannedId, Boolean.TRUE);
          myLastScannedId = 0;
        }
      }
      super.addValue(key, inputId, value);
    }
    catch (IOException e) {
      throw new StorageException(e);
    }
  }

  private static class IntPairInArrayKeyDescriptor implements KeyDescriptor<int[]>, DifferentSerializableBytesImplyNonEqualityPolicy {
    private static final IntPairInArrayKeyDescriptor INSTANCE = new IntPairInArrayKeyDescriptor();
    @Override
    public void save(@NotNull DataOutput out, int[] value) throws IOException {
      DataInputOutputUtil.writeINT(out, value[0]);
      DataInputOutputUtil.writeINT(out, value[1]);
    }

    @Override
    public int[] read(@NotNull DataInput in) throws IOException {
      return new int[] {DataInputOutputUtil.readINT(in), DataInputOutputUtil.readINT(in)};
    }

    @Override
    public int getHashCode(int[] value) {
      return value[0] * 31 + value[1];
    }

    @Override
    public boolean isEqual(int[] val1, int[] val2) {
      return val1[0] == val2[0] && val1[1] == val2[1];
    }
  }
}
