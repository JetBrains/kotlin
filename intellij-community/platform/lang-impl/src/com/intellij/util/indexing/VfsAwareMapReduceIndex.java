// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.util.indexing;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.TransactionGuard;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.util.io.ByteArraySequence;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.impl.cache.impl.id.IdIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.Processor;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.indexing.impl.*;
import com.intellij.util.indexing.impl.forward.AbstractMapForwardIndexAccessor;
import com.intellij.util.indexing.impl.forward.ForwardIndex;
import com.intellij.util.indexing.impl.forward.ForwardIndexAccessor;
import com.intellij.util.indexing.impl.forward.MapForwardIndexAccessor;
import com.intellij.util.indexing.impl.forward.PersistentMapBasedForwardIndex;
import gnu.trove.THashSet;
import gnu.trove.TIntObjectHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;

/**
 * @author Eugene Zhuravlev
 */
public class VfsAwareMapReduceIndex<Key, Value, Input> extends MapReduceIndex<Key, Value, Input> implements UpdatableIndex<Key, Value, Input>{
  private static final Logger LOG = Logger.getInstance(VfsAwareMapReduceIndex.class);

  static {
    if (!DebugAssertions.DEBUG) {
      final Application app = ApplicationManager.getApplication();
      DebugAssertions.DEBUG = app.isEAP() || app.isInternal();
    }
  }

  private final AtomicBoolean myInMemoryMode = new AtomicBoolean();
  private final TIntObjectHashMap<Map<Key, Value>> myInMemoryKeysAndValues = new TIntObjectHashMap<>();

  private final SnapshotInputMappingIndex<Key, Value, Input> mySnapshotInputMappings;
  private final boolean myUpdateMappings;

  public VfsAwareMapReduceIndex(@NotNull IndexExtension<Key, Value, Input> extension,
                                @NotNull IndexStorage<Key, Value> storage) throws IOException {
    this(extension,
         storage,
         hasSnapshotMapping(extension) ? new SnapshotInputMappings<>(extension) : null);
    if (!(myIndexId instanceof ID<?, ?>)) {
      throw new IllegalArgumentException("myIndexId should be instance of com.intellij.util.indexing.ID");
    }
  }

  public VfsAwareMapReduceIndex(@NotNull IndexExtension<Key, Value, Input> extension,
                                @NotNull IndexStorage<Key, Value> storage,
                                @Nullable SnapshotInputMappings<Key, Value, Input> snapshotInputMappings) throws IOException {
    this(extension,
         storage,
         snapshotInputMappings != null ? new SharedIntMapForwardIndex(extension, snapshotInputMappings.getInputIndexStorageFile(), true)
                                       : getForwardIndexMap(extension),
         snapshotInputMappings != null ? snapshotInputMappings.getForwardIndexAccessor() : getForwardIndexAccessor(extension),
         snapshotInputMappings);
  }

  protected VfsAwareMapReduceIndex(@NotNull IndexExtension<Key, Value, Input> extension,
                                   @NotNull IndexStorage<Key, Value> storage,
                                   @Nullable ForwardIndex forwardIndexMap,
                                   @Nullable ForwardIndexAccessor<Key, Value> forwardIndexAccessor,
                                   @Nullable SnapshotInputMappingIndex<Key, Value, Input> snapshotInputMappings) {
    super(extension, storage, forwardIndexMap, forwardIndexAccessor, null);
    SharedIndicesData.registerIndex((ID<Key, Value>)myIndexId, extension);
    mySnapshotInputMappings = IndexImporterMappingIndex.wrap(snapshotInputMappings, extension);
    myUpdateMappings = snapshotInputMappings instanceof UpdatableSnapshotInputMappingIndex;
    installMemoryModeListener();
  }

  private static <Key, Value> boolean hasSnapshotMapping(@NotNull IndexExtension<Key, Value, ?> indexExtension) {
    return indexExtension instanceof FileBasedIndexExtension &&
           ((FileBasedIndexExtension<Key, Value>)indexExtension).hasSnapshotMapping() &&
           IdIndex.ourSnapshotMappingsEnabled;
  }

  @NotNull
  @Override
  protected InputData<Key, Value> mapInput(@Nullable Input content) {
    InputData<Key, Value> data;
    boolean containsSnapshotData = true;
    if (mySnapshotInputMappings != null && content != null) {
      try {
        data = mySnapshotInputMappings.readData(content);
        if (data != null) {
          return data;
        } else {
          containsSnapshotData = !myUpdateMappings;
        }
      }
      catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    data = super.mapInput(content);
    if (!containsSnapshotData && !UpdatableSnapshotInputMappingIndex.ignoreMappingIndexUpdate(content)) {
      try {
        return ((UpdatableSnapshotInputMappingIndex)mySnapshotInputMappings).putData(content, data);
      }
      catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    return data;
  }

  @NotNull
  @Override
  protected InputDataDiffBuilder<Key, Value> getKeysDiffBuilder(int inputId) throws IOException {
    if (mySnapshotInputMappings != null && !myInMemoryMode.get()) {
      return super.getKeysDiffBuilder(inputId);
    }
    if (myInMemoryMode.get()) {
      synchronized (myInMemoryKeysAndValues) {
        Map<Key, Value> keysAndValues = myInMemoryKeysAndValues.get(inputId);
        if (keysAndValues != null) {
          return getKeysDiffBuilderInMemoryMode(inputId, keysAndValues);
        }
      }
    }
    return super.getKeysDiffBuilder(inputId);
  }

  @NotNull
  protected InputDataDiffBuilder<Key, Value> getKeysDiffBuilderInMemoryMode(int inputId, @NotNull Map<Key, Value> keysAndValues) {
    return new MapInputDataDiffBuilder<>(inputId, keysAndValues);
  }

  @Override
  protected void updateForwardIndex(int inputId, @NotNull InputData<Key, Value> data) throws IOException {
    if (myInMemoryMode.get()) {
      synchronized (myInMemoryKeysAndValues) {
        myInMemoryKeysAndValues.put(inputId, data.getKeyValues());
      }
    } else {
      super.updateForwardIndex(inputId, data);
    }
  }

  @Override
  public void setIndexedStateForFile(int fileId, @NotNull VirtualFile file) {
    IndexingStamp.setFileIndexedStateCurrent(fileId, (ID<?, ?>)myIndexId);
  }

  @Override
  public void resetIndexedStateForFile(int fileId) {
    IndexingStamp.setFileIndexedStateOutdated(fileId, (ID<?, ?>)myIndexId);
  }

  @Override
  public boolean isIndexedStateForFile(int fileId, @NotNull VirtualFile file) {
    return IndexingStamp.isFileIndexedStateCurrent(fileId, (ID<?, ?>)myIndexId);
  }

  @Override
  public void removeTransientDataForFile(int inputId) {
    Lock lock = getWriteLock();
    lock.lock();
    try {
      Map<Key, Value> keyValueMap;
      synchronized (myInMemoryKeysAndValues) {
        keyValueMap = myInMemoryKeysAndValues.remove(inputId);
      }

      if (keyValueMap == null) return;

      try {
        removeTransientDataForInMemoryKeys(inputId, keyValueMap);

        InputDataDiffBuilder<Key, Value> builder = getKeysDiffBuilder(inputId);
        if (builder instanceof CollectionInputDataDiffBuilder<?, ?>) {
          Collection<Key> keyCollectionFromDisk = ((CollectionInputDataDiffBuilder<Key, Value>)builder).getSeq();
          if (keyCollectionFromDisk != null) {
            removeTransientDataForKeys(inputId, keyCollectionFromDisk);
          }
        } else {
          Set<Key> diskKeySet = new THashSet<>();

          builder.differentiate(
            Collections.emptyMap(),
            (key, value, inputId1) -> {
            },
            (key, value, inputId1) -> {},
            (key, inputId1) -> diskKeySet.add(key)
          );
          removeTransientDataForKeys(inputId, diskKeySet);
        }
      } catch (Throwable throwable) {
        throw new RuntimeException(throwable);
      }
    } finally {
      lock.unlock();
    }
  }

  protected void removeTransientDataForInMemoryKeys(int inputId, @NotNull Map<? extends Key, ? extends Value> map) {
    removeTransientDataForKeys(inputId, map.keySet());
  }

  public void removeTransientDataForKeys(int inputId, @NotNull Collection<? extends Key> keys) {
    MemoryIndexStorage memoryIndexStorage = (MemoryIndexStorage)getStorage();
    for (Key key : keys) {
      memoryIndexStorage.clearMemoryMapForId(key, inputId);
    }
  }

  @Override
  public boolean processAllKeys(@NotNull Processor<? super Key> processor, @NotNull GlobalSearchScope scope, @Nullable IdFilter idFilter) throws StorageException {
    final Lock lock = getReadLock();
    lock.lock();
    try {
      return ((VfsAwareIndexStorage<Key, Value>)myStorage).processKeys(processor, scope, idFilter);
    }
    finally {
      lock.unlock();
    }
  }

  @NotNull
  @Override
  public Map<Key, Value> getIndexedFileData(int fileId) throws StorageException {
    try {
      return Collections.unmodifiableMap(ContainerUtil.notNullize(getNullableIndexedData(fileId)));
    }
    catch (IOException e) {
      throw new StorageException(e);
    }
  }

  @Nullable
  private Map<Key, Value> getNullableIndexedData(int fileId) throws IOException, StorageException {
    if (myInMemoryMode.get()) {
      Map<Key, Value> map = myInMemoryKeysAndValues.get(fileId);
      if (map != null) return map;
    }
    if (getForwardIndexAccessor() instanceof AbstractMapForwardIndexAccessor) {
      ByteArraySequence serializedInputData = getForwardIndexMap().get(fileId);
      AbstractMapForwardIndexAccessor<Key, Value, ?> forwardIndexAccessor = (AbstractMapForwardIndexAccessor<Key, Value, ?>)getForwardIndexAccessor();
      return forwardIndexAccessor.convertToInputDataMap(serializedInputData);
    }
    // in future we will get rid of forward index for SingleEntryFileBasedIndexExtension
    if (myExtension instanceof SingleEntryFileBasedIndexExtension) {
      Key key = (Key)(Object)fileId;
      final Map<Key, Value>[] result = new Map[]{Collections.emptyMap()};
      ValueContainer<Value> container = getData(key);
      container.forEach((id, value) -> {
        result[0] = Collections.singletonMap(key, value);
        return false;
      });
      return result[0];
    }
    LOG.error("Can't fetch indexed data for index " + myIndexId.getName());
    return null;
  }

  @Override
  public void checkCanceled() {
    ProgressManager.checkCanceled();
  }

  @Override
  protected void requestRebuild(@NotNull Throwable ex) {
    Runnable action = () -> FileBasedIndex.getInstance().requestRebuild((ID<?, ?>)myIndexId, ex);
    Application application = ApplicationManager.getApplication();
    if (application.isUnitTestMode() || application.isHeadlessEnvironment()) {
      // avoid deadlock due to synchronous update in DumbServiceImpl#queueTask
      TransactionGuard.getInstance().submitTransactionLater(application, action);
    } else {
      action.run();
    }
  }

  @Override
  protected void doClear() throws StorageException, IOException {
    super.doClear();
    if (mySnapshotInputMappings != null && myUpdateMappings) {
      try {
        ((UpdatableSnapshotInputMappingIndex)mySnapshotInputMappings).clear();
      }
      catch (IOException e) {
        LOG.error(e);
      }
    }
  }

  @Override
  protected void doFlush() throws IOException, StorageException {
    super.doFlush();
    if (mySnapshotInputMappings != null && myUpdateMappings) {
      ((UpdatableSnapshotInputMappingIndex)mySnapshotInputMappings).flush();
    }
  }

  @Override
  protected void doDispose() throws StorageException {
    super.doDispose();

    if (mySnapshotInputMappings != null) {
      try {
        mySnapshotInputMappings.close();
      }
      catch (IOException e) {
        LOG.error(e);
      }
    }
  }

  @Nullable
  private static <Key, Value> ForwardIndexAccessor<Key, Value> getForwardIndexAccessor(@NotNull IndexExtension<Key, Value, ?> indexExtension) {
    if (!shouldCreateForwardIndex(indexExtension)) return null;
    return new MapForwardIndexAccessor<>(new InputMapExternalizer<>(indexExtension));
  }

  @Nullable
  private static ForwardIndex getForwardIndexMap(@NotNull IndexExtension<?, ?, ?> indexExtension)
    throws IOException {
    if (!shouldCreateForwardIndex(indexExtension)) return null;
    File indexStorageFile = IndexInfrastructure.getInputIndexStorageFile((ID<?, ?>)indexExtension.getName());
    return new PersistentMapBasedForwardIndex(indexStorageFile, false);
  }

  private static boolean shouldCreateForwardIndex(@NotNull IndexExtension<?, ?, ?> indexExtension) {
    if (hasSnapshotMapping(indexExtension)) return false;
    if (indexExtension instanceof CustomInputsIndexFileBasedIndexExtension) {
      LOG.error("Index `" + indexExtension.getName() + "` will be created without forward index");
      return false;
    }
    return true;
  }

  private void installMemoryModeListener() {
    IndexStorage<Key, Value> storage = getStorage();
    if (storage instanceof MemoryIndexStorage) {
      ((MemoryIndexStorage)storage).addBufferingStateListener(new MemoryIndexStorage.BufferingStateListener() {
        @Override
        public void bufferingStateChanged(boolean newState) {
          myInMemoryMode.set(newState);
        }

        @Override
        public void memoryStorageCleared() {
          synchronized (myInMemoryKeysAndValues) {
            myInMemoryKeysAndValues.clear();
          }
        }
      });
    }
  }
}
