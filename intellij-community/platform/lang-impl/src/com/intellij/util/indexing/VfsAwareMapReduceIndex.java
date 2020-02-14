// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.util.indexing;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.impl.ApplicationInfoImpl;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.util.io.ByteArraySequence;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.ConcurrencyUtil;
import com.intellij.util.Processor;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.indexing.impl.*;
import com.intellij.util.indexing.impl.forward.*;
import com.intellij.util.indexing.impl.perFileVersion.PersistentSubIndexerRetriever;
import com.intellij.util.indexing.snapshot.*;
import com.intellij.util.io.IOUtil;
import gnu.trove.THashSet;
import gnu.trove.TIntObjectHashMap;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * @author Eugene Zhuravlev
 */
public class VfsAwareMapReduceIndex<Key, Value> extends MapReduceIndex<Key, Value, FileContent> implements UpdatableIndex<Key, Value, FileContent>{
  private static final Logger LOG = Logger.getInstance(VfsAwareMapReduceIndex.class);

  @ApiStatus.Internal
  public static final int VERSION = 0;

  static {
    if (!DebugAssertions.DEBUG) {
      final Application app = ApplicationManager.getApplication();
      DebugAssertions.DEBUG = (app.isEAP() || app.isInternal()) && !ApplicationInfoImpl.isInStressTest();
    }
  }

  private final AtomicBoolean myInMemoryMode = new AtomicBoolean();
  private final TIntObjectHashMap<Map<Key, Value>> myInMemoryKeysAndValues = new TIntObjectHashMap<>();

  @SuppressWarnings("rawtypes")
  private final PersistentSubIndexerRetriever mySubIndexerRetriever;
  private final SnapshotInputMappingIndex<Key, Value, FileContent> mySnapshotInputMappings;
  private final boolean myUpdateMappings;
  private final boolean mySingleEntryIndex;

  public VfsAwareMapReduceIndex(@NotNull IndexExtension<Key, Value, FileContent> extension,
                                @NotNull IndexStorage<Key, Value> storage) throws IOException {
    this(extension,
         storage,
         hasSnapshotMapping(extension) ? new SnapshotInputMappings<>(extension) : null);
    if (!(myIndexId instanceof ID<?, ?>)) {
      throw new IllegalArgumentException("myIndexId should be instance of com.intellij.util.indexing.ID");
    }
  }

  public VfsAwareMapReduceIndex(@NotNull IndexExtension<Key, Value, FileContent> extension,
                                @NotNull IndexStorage<Key, Value> storage,
                                @Nullable SnapshotInputMappings<Key, Value> snapshotInputMappings) throws IOException {
    this(extension,
         storage,
         snapshotInputMappings != null ? new SharedIntMapForwardIndex(extension, snapshotInputMappings.getInputIndexStorageFile(), true)
                                       : getForwardIndexMap(extension),
         snapshotInputMappings != null ? snapshotInputMappings.getForwardIndexAccessor() : getForwardIndexAccessor(extension),
         snapshotInputMappings, null);
  }

  public VfsAwareMapReduceIndex(@NotNull IndexExtension<Key, Value, FileContent> extension,
                                @NotNull IndexStorage<Key, Value> storage,
                                @Nullable ForwardIndex forwardIndexMap,
                                @Nullable ForwardIndexAccessor<Key, Value> forwardIndexAccessor,
                                @Nullable SnapshotInputMappings<Key, Value> snapshotInputMappings,
                                @Nullable ReadWriteLock lock) {
    super(extension, storage, forwardIndexMap, forwardIndexAccessor, lock);
    if (myIndexId instanceof ID) {
      SharedIndicesData.registerIndex((ID<Key, Value>)myIndexId, extension);
    }
    if (storage instanceof MemoryIndexStorage && snapshotInputMappings != null) {
      VfsAwareIndexStorage<Key, Value> backendStorage = ((MemoryIndexStorage<Key, Value>)storage).getBackendStorage();
      if (backendStorage instanceof SnapshotSingleValueIndexStorage) {
        LOG.assertTrue(forwardIndexMap instanceof IntForwardIndex);
        ((SnapshotSingleValueIndexStorage<Key, Value>)backendStorage).init(snapshotInputMappings, ((IntForwardIndex)forwardIndexMap));
      }
    }
    if (isCompositeIndexer(myIndexer)) {
      try {
        //noinspection unchecked,rawtypes,ConstantConditions
        mySubIndexerRetriever = new PersistentSubIndexerRetriever((ID)myIndexId,
                                                                  extension.getVersion(),
                                                                  (CompositeDataIndexer) myIndexer);
        if (snapshotInputMappings != null) {
          snapshotInputMappings.setSubIndexerRetriever(mySubIndexerRetriever);
        }
      }
      catch (IOException e) {
        throw new RuntimeException(e);
      }
    } else {
      mySubIndexerRetriever = null;
    }
    mySnapshotInputMappings = IndexImporterMappingIndex.wrap(snapshotInputMappings, extension);
    myUpdateMappings = mySnapshotInputMappings instanceof UpdatableSnapshotInputMappingIndex;
    mySingleEntryIndex = extension instanceof SingleEntryFileBasedIndexExtension;
    installMemoryModeListener();
  }

  public static boolean isCompositeIndexer(@NotNull DataIndexer<?, ?, ?> indexer) {
    return indexer instanceof CompositeDataIndexer && InvertedIndex.ARE_COMPOSITE_INDEXERS_ENABLED;
  }

  static <Key, Value> boolean hasSnapshotMapping(@NotNull IndexExtension<Key, Value, ?> indexExtension) {
    return indexExtension instanceof FileBasedIndexExtension &&
           ((FileBasedIndexExtension<Key, Value>)indexExtension).hasSnapshotMapping() &&
           FileBasedIndex.ourSnapshotMappingsEnabled;
  }

  @NotNull
  @Override
  protected InputData<Key, Value> mapInput(int inputId, @Nullable FileContent content) {
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
    data = super.mapInput(inputId, content);
    if (!containsSnapshotData && !UpdatableSnapshotInputMappingIndex.ignoreMappingIndexUpdate(content)) {
      try {
        return ((UpdatableSnapshotInputMappingIndex<Key, Value, FileContent>)mySnapshotInputMappings).putData(content, data);
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
    return mySingleEntryIndex ? new SingleEntryIndexForwardIndexAccessor.SingleValueDiffBuilder(inputId, keysAndValues)
                              : new MapInputDataDiffBuilder<>(inputId, keysAndValues);
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
  public void setIndexedStateForFile(int fileId, @NotNull IndexedFile file) {
    IndexingStamp.setFileIndexedStateCurrent(fileId, (ID<?, ?>)myIndexId);
    if (mySubIndexerRetriever != null) {
      try {
        mySubIndexerRetriever.setIndexedState(fileId, file);
      }
      catch (IOException e) {
        LOG.error(e);
      }
    }
  }

  @Override
  public void resetIndexedStateForFile(int fileId) {
    IndexingStamp.setFileIndexedStateOutdated(fileId, (ID<?, ?>)myIndexId);
  }

  @Override
  public boolean isIndexedStateForFile(int fileId, @NotNull IndexedFile file) {
    if (!IndexingStamp.isFileIndexedStateCurrent(fileId, (ID<?, ?>)myIndexId)) {
      return false;
    }
    if (mySubIndexerRetriever == null) return true;
    if (!(file instanceof FileContent)) {
      if (((CompositeDataIndexer)myIndexer).requiresContentForSubIndexerEvaluation(file)) {
        return isIndexConfigurationUpToDate(fileId, file);
      }
    }
    try {
      return mySubIndexerRetriever.isIndexed(fileId, file);
    }
    catch (IOException e) {
      LOG.error(e);
      return false;
    }
  }

  protected boolean isIndexConfigurationUpToDate(int fileId, @NotNull IndexedFile file) {
    return false;
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
        removeTransientDataForKeys(inputId, getKeys(builder));
      } catch (StorageException | IOException throwable) {
        throw new RuntimeException(throwable);
      }
    } finally {
      lock.unlock();
    }
  }

  protected void removeTransientDataForInMemoryKeys(int inputId, @NotNull Map<? extends Key, ? extends Value> map) {
    removeTransientDataForKeys(inputId, map.keySet());
  }

  @Override
  public void removeTransientDataForKeys(int inputId, @NotNull Collection<? extends Key> keys) {
    MemoryIndexStorage<Key, Value> memoryIndexStorage = (MemoryIndexStorage<Key, Value>)getStorage();
    boolean modified = false;
    for (Key key : keys) {
      if (memoryIndexStorage.clearMemoryMapForId(key, inputId) && !modified) {
        modified = true;
      }
    }
    if (modified) {
      myModificationStamp.incrementAndGet();
    }
  }


  @Override
  public void setBufferingEnabled(boolean enabled) {
    ((MemoryIndexStorage<Key, Value>)getStorage()).setBufferingEnabled(enabled);
  }

  @Override
  public void cleanupMemoryStorage() {
    MemoryIndexStorage<Key, Value> memStorage = (MemoryIndexStorage<Key, Value>)getStorage();
    ConcurrencyUtil.withLock(getWriteLock(), () -> {
      if (memStorage.clearMemoryMap()) {
        myModificationStamp.incrementAndGet();
      }
    });
    memStorage.fireMemoryStorageCleared();
  }

  @TestOnly
  @Override
  public void cleanupForNextTest() {
    MemoryIndexStorage<Key, Value> memStorage = (MemoryIndexStorage<Key, Value>)getStorage();
    ConcurrencyUtil.withLock(getReadLock(), () -> memStorage.clearCaches());
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
    return ConcurrencyUtil.withLock(getReadLock(), () -> {
      try {
        return Collections.unmodifiableMap(ContainerUtil.notNullize(getNullableIndexedData(fileId)));
      }
      catch (IOException e) {
        throw new StorageException(e);
      }
    });
  }

  @Nullable
  private Map<Key, Value> getNullableIndexedData(int fileId) throws IOException, StorageException {
    if (myInMemoryMode.get()) {
      Map<Key, Value> map = myInMemoryKeysAndValues.get(fileId);
      if (map != null) return map;
    }
    if (getForwardIndexAccessor() instanceof AbstractMapForwardIndexAccessor) {
      ByteArraySequence serializedInputData = getForwardIndex().get(fileId);
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
      ApplicationManager.getApplication().invokeLater(action);
    } else {
      action.run();
    }
  }

  @Override
  protected void doClear() throws StorageException, IOException {
    super.doClear();
    if (mySnapshotInputMappings != null && myUpdateMappings) {
      try {
        ((UpdatableSnapshotInputMappingIndex<Key, Value, FileContent>)mySnapshotInputMappings).clear();
      }
      catch (IOException e) {
        LOG.error(e);
      }
    }
    if (mySubIndexerRetriever != null) {
      try {
        mySubIndexerRetriever.clear();
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
      ((UpdatableSnapshotInputMappingIndex<Key, Value, FileContent>)mySnapshotInputMappings).flush();
    }
    if (mySubIndexerRetriever != null) {
      mySubIndexerRetriever.flush();
    }
  }

  @Override
  protected void doDispose() throws StorageException {
    try {
      super.doDispose();
    } finally {
      IOUtil.closeSafe(LOG, mySnapshotInputMappings, mySubIndexerRetriever);
    }
  }

  @Nullable
  private static <Key, Value> ForwardIndexAccessor<Key, Value> getForwardIndexAccessor(@NotNull IndexExtension<Key, Value, ?> indexExtension) {
    if (!shouldCreateForwardIndex(indexExtension)) return null;
    if (indexExtension instanceof SingleEntryFileBasedIndexExtension) return new SingleEntryIndexForwardIndexAccessor(indexExtension);
    return new MapForwardIndexAccessor<>(new InputMapExternalizer<>(indexExtension));
  }

  @Nullable
  private static ForwardIndex getForwardIndexMap(@NotNull IndexExtension<?, ?, ?> indexExtension)
    throws IOException {
    if (!shouldCreateForwardIndex(indexExtension)) return null;
    if (indexExtension instanceof SingleEntryFileBasedIndexExtension<?>) return new EmptyForwardIndex(); // indexStorage and forwardIndex are same here
    File indexStorageFile = IndexInfrastructure.getInputIndexStorageFile((ID<?, ?>)indexExtension.getName());
    return new PersistentMapBasedForwardIndex(indexStorageFile.toPath(), false, false);
  }

  private static boolean shouldCreateForwardIndex(@NotNull IndexExtension<?, ?, ?> indexExtension) {
    return !hasSnapshotMapping(indexExtension);
  }

  private void installMemoryModeListener() {
    IndexStorage<Key, Value> storage = getStorage();
    if (storage instanceof MemoryIndexStorage) {
      ((MemoryIndexStorage<Key, Value>)storage).addBufferingStateListener(new MemoryIndexStorage.BufferingStateListener() {
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

  @NotNull
  private Collection<Key> getKeys(InputDataDiffBuilder<Key, ?> builder) throws StorageException {
    if (builder instanceof DirectInputDataDiffBuilder<?, ?>) {
      return ((DirectInputDataDiffBuilder<Key, ?>)builder).getKeys();
    }

    LOG.error("Input data diff builder must be an instance of DirectInputDataDiffBuilder for index " + myIndexId.getName());

    Set<Key> keys = new THashSet<>();
    builder.differentiate(
      Collections.emptyMap(),
      (key, value, inputId) -> { },
      (key, value, inputId) -> {},
      (key, inputId) -> keys.add(key)
    );
    return keys;
  }
}
