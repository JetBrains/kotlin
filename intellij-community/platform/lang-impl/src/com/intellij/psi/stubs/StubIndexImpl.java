// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

/*
 * @author max
 */
package com.intellij.psi.stubs;

import com.intellij.openapi.application.AppUIExecutor;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.components.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.ExtensionPointListener;
import com.intellij.openapi.extensions.PluginDescriptor;
import com.intellij.openapi.extensions.impl.ExtensionPointImpl;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.util.ProgressIndicatorUtils;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.ModificationTracker;
import com.intellij.openapi.util.io.BufferExposingByteArrayOutputStream;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.ManagingFS;
import com.intellij.openapi.vfs.newvfs.persistent.PersistentFS;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.util.CachedValueImpl;
import com.intellij.util.ConcurrencyUtil;
import com.intellij.util.Processor;
import com.intellij.util.Processors;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.FactoryMap;
import com.intellij.util.indexing.*;
import com.intellij.util.indexing.impl.AbstractUpdateData;
import com.intellij.util.indexing.impl.KeyValueUpdateProcessor;
import com.intellij.util.indexing.impl.RemovedKeyProcessor;
import com.intellij.util.io.DataOutputStream;
import com.intellij.util.io.*;
import gnu.trove.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.io.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.function.IntPredicate;

@State(name = "FileBasedIndex", storages = {
  @Storage(value = StoragePathMacros.CACHE_FILE),
  @Storage(value = "stubIndex.xml", deprecated = true, roamingType = RoamingType.DISABLED)
})
public final class StubIndexImpl extends StubIndexEx implements PersistentStateComponent<StubIndexState> {
  private static final AtomicReference<Boolean> ourForcedClean = new AtomicReference<>(null);
  static final Logger LOG = Logger.getInstance(StubIndexImpl.class);

  private static class AsyncState {
    private final Map<StubIndexKey<?, ?>, UpdatableIndex<?, Void, FileContent>> myIndices = new THashMap<>();
    private final Map<StubIndexKey<?, ?>, TObjectHashingStrategy<?>> myKeyHashingStrategies = new THashMap<>();
    private final TObjectIntHashMap<ID<?, ?>> myIndexIdToVersionMap = new TObjectIntHashMap<>();
  }

  private final Map<StubIndexKey<?, ?>, CachedValue<Map<CompositeKey<?>, StubIdList>>> myCachedStubIds = FactoryMap.createMap(k -> {
    UpdatableIndex<Integer, SerializedStubTree, FileContent> index = getStubUpdatingIndex();
    ModificationTracker tracker = index::getModificationStamp;
    return new CachedValueImpl<>(() -> new CachedValueProvider.Result<>(ContainerUtil.newConcurrentMap(), tracker));
  }, ContainerUtil::newConcurrentMap);

  private final StubProcessingHelper myStubProcessingHelper = new StubProcessingHelper();
  private final IndexAccessValidator myAccessValidator = new IndexAccessValidator();
  private volatile CompletableFuture<AsyncState> myStateFuture;
  private volatile AsyncState myState;
  private volatile boolean myInitialized;

  private StubIndexState myPreviouslyRegistered;

  public StubIndexImpl() {
    StubIndexExtension.EP_NAME.addExtensionPointListener(new ExtensionPointListener<StubIndexExtension<?, ?>>() {
      @Override
      public void extensionRemoved(@NotNull StubIndexExtension<?, ?> extension, @NotNull PluginDescriptor pluginDescriptor) {
        ID.unloadId(extension.getKey());
      }
    }, ApplicationManager.getApplication());
  }

  @Nullable
  static StubIndexImpl getInstanceOrInvalidate() {
    if (ourForcedClean.compareAndSet(null, Boolean.TRUE)) {
      return null;
    }
    return (StubIndexImpl)getInstance();
  }

  private AsyncState getAsyncState() {
    AsyncState state = myState; // memory barrier
    if (state == null) {
      if (myStateFuture == null) {
        ((FileBasedIndexImpl)FileBasedIndex.getInstance()).waitUntilIndicesAreInitialized();
      }
      myState = state = ProgressIndicatorUtils.awaitWithCheckCanceled(myStateFuture);
    }
    return state;
  }

  @NotNull
  public static <K> FileBasedIndexExtension<K, Void> wrapStubIndexExtension(StubIndexExtension<K, ?> extension) {
    return new FileBasedIndexExtension<K, Void>() {
      @NotNull
      @Override
      public ID<K, Void> getName() {
        //noinspection unchecked
        return (ID<K, Void>)extension.getKey();
      }

      @NotNull
      @Override
      public FileBasedIndex.InputFilter getInputFilter() {
        return f -> {
          throw new UnsupportedOperationException();
        };
      }

      @Override
      public boolean dependsOnFileContent() {
        return true;
      }

      @NotNull
      @Override
      public DataIndexer<K, Void, FileContent> getIndexer() {
        return i -> {
          throw new AssertionError();
        };
      }

      @NotNull
      @Override
      public KeyDescriptor<K> getKeyDescriptor() {
        return extension.getKeyDescriptor();
      }

      @NotNull
      @Override
      public DataExternalizer<Void> getValueExternalizer() {
        return VoidDataExternalizer.INSTANCE;
      }

      @Override
      public int getVersion() {
        return extension.getVersion();
      }

      @Override
      public boolean traceKeyHashToVirtualFileMapping() {
        return extension instanceof StringStubIndexExtension && ((StringStubIndexExtension<?>)extension).traceKeyHashToVirtualFileMapping();
      }
    };
  }

  @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
  private static <K> void registerIndexer(@NotNull final StubIndexExtension<K, ?> extension, final boolean forceClean,
                                          @NotNull AsyncState state, @NotNull IndicesRegistrationResult registrationResultSink)
    throws IOException {
    final StubIndexKey<K, ?> indexKey = extension.getKey();
    final int version = extension.getVersion();
    FileBasedIndexExtension<K, Void> wrappedExtension = wrapStubIndexExtension(extension);
    synchronized (state) {
      state.myIndexIdToVersionMap.put(indexKey, version);
    }

    final File indexRootDir = IndexInfrastructure.getIndexRootDir(indexKey);

    if (forceClean || IndexingStamp.versionDiffers(indexKey, version)) {
      final File versionFile = IndexInfrastructure.getVersionFile(indexKey);
      final boolean versionFileExisted = versionFile.exists();

      final String[] children = indexRootDir.list();
      // rebuild only if there exists what to rebuild
      boolean indexRootHasChildren = children != null && children.length > 0;
      boolean needRebuild = !forceClean && (versionFileExisted || indexRootHasChildren);

      if (needRebuild) registrationResultSink.registerIndexAsChanged(indexKey);
      else registrationResultSink.registerIndexAsInitiallyBuilt(indexKey);
      if (indexRootHasChildren) FileUtil.deleteWithRenaming(indexRootDir);
      IndexingStamp.rewriteVersion(indexKey, version); // todo snapshots indices

      try {
        if (needRebuild) {
          for (FileBasedIndexInfrastructureExtension ex : FileBasedIndexInfrastructureExtension.EP_NAME.getExtensionList()) {
            ex.onStubIndexVersionChanged(indexKey);
          }
        }
      } catch (Exception e) {
        LOG.error(e);
      }

    } else {
      registrationResultSink.registerIndexAsUptoDate(indexKey);
    }
    UpdatableIndex<Integer, SerializedStubTree, FileContent> stubUpdatingIndex = getStubUpdatingIndex();
    ReadWriteLock lock = stubUpdatingIndex.getLock();

    for (int attempt = 0; attempt < 2; attempt++) {
      try {
        final VfsAwareMapIndexStorage<K, Void> storage = new VfsAwareMapIndexStorage<>(
          IndexInfrastructure.getStorageFile(indexKey).toPath(),
          wrappedExtension.getKeyDescriptor(),
          wrappedExtension.getValueExternalizer(),
          wrappedExtension.getCacheSize(),
          wrappedExtension.keyIsUniqueForIndexedFile(),
          wrappedExtension.traceKeyHashToVirtualFileMapping()
        );
        final MemoryIndexStorage<K, Void> memStorage = new MemoryIndexStorage<>(storage, indexKey);
        UpdatableIndex<K, Void, FileContent> index = new VfsAwareMapReduceIndex<>(wrappedExtension, memStorage, null, null, null, lock);

        for (FileBasedIndexInfrastructureExtension infrastructureExtension : FileBasedIndexInfrastructureExtension.EP_NAME.getExtensionList()) {
          UpdatableIndex<K, Void, FileContent> intermediateIndex = infrastructureExtension.combineIndex(wrappedExtension, index);
          if (intermediateIndex != null) {
            index = intermediateIndex;
          }
        }

        TObjectHashingStrategy<K> keyHashingStrategy = new TObjectHashingStrategy<K>() {
          private final KeyDescriptor<K> descriptor = extension.getKeyDescriptor();

          @Override
          public int computeHashCode(K object) {
            return descriptor.getHashCode(object);
          }

          @Override
          public boolean equals(K o1, K o2) {
            return descriptor.isEqual(o1, o2);
          }
        };

        synchronized (state) {
          state.myIndices.put(indexKey, index);
          state.myKeyHashingStrategies.put(indexKey, keyHashingStrategy);
        }
        break;
      }
      catch (IOException e) {
        registrationResultSink.registerIndexAsInitiallyBuilt(indexKey);
        onExceptionInstantiatingIndex(indexKey, version, indexRootDir, e);
      }
      catch (RuntimeException e) {
        Throwable cause = FileBasedIndexImpl.getCauseToRebuildIndex(e);
        if (cause == null) throw e;
        onExceptionInstantiatingIndex(indexKey, version, indexRootDir, e);
      }
    }
  }

  @Override
  @NotNull
  <K> TObjectHashingStrategy<K> getKeyHashingStrategy(StubIndexKey<K, ?> stubIndexKey) {
    //noinspection unchecked
    return (TObjectHashingStrategy<K>)getAsyncState().myKeyHashingStrategies.get(stubIndexKey);
  }

  private static <K> void onExceptionInstantiatingIndex(@NotNull StubIndexKey<K, ?> indexKey,
                                                        int version,
                                                        @NotNull File indexRootDir,
                                                        @NotNull Exception e) throws IOException {
    LOG.info(e);
    FileUtil.deleteWithRenaming(indexRootDir);
    IndexingStamp.rewriteVersion(indexKey, version); // todo snapshots indices
  }

  public long getIndexModificationStamp(@NotNull StubIndexKey<?, ?> indexId, @NotNull Project project) {
    UpdatableIndex<?, Void, FileContent> index = getAsyncState().myIndices.get(indexId);
    if (index != null) {
      FileBasedIndex.getInstance().ensureUpToDate(StubUpdatingIndex.INDEX_ID, project, GlobalSearchScope.allScope(project));
      return index.getModificationStamp();
    }
    return -1;
  }

  public void flush() throws StorageException {
    if (!myInitialized) {
      return;
    }
    for (UpdatableIndex<?, Void, FileContent> index : getAsyncState().myIndices.values()) {
      index.flush();
    }
  }

  public static class StubIdExternalizer implements DataExternalizer<StubIdList> {
    public static final StubIdExternalizer INSTANCE = new StubIdExternalizer();

    @Override
    public void save(@NotNull final DataOutput out, @NotNull final StubIdList value) throws IOException {
      int size = value.size();
      if (size == 0) {
        DataInputOutputUtil.writeINT(out, Integer.MAX_VALUE);
      }
      else if (size == 1) {
        DataInputOutputUtil.writeINT(out, value.get(0)); // most often case
      }
      else {
        DataInputOutputUtil.writeINT(out, -size);
        for (int i = 0; i < size; ++i) {
          DataInputOutputUtil.writeINT(out, value.get(i));
        }
      }
    }

    @NotNull
    @Override
    public StubIdList read(@NotNull final DataInput in) throws IOException {
      int size = DataInputOutputUtil.readINT(in);
      if (size == Integer.MAX_VALUE) {
        return new StubIdList();
      }
      if (size >= 0) {
        return new StubIdList(size);
      }
      size = -size;
      int[] result = new int[size];
      for (int i = 0; i < size; ++i) {
        result[i] = DataInputOutputUtil.readINT(in);
      }
      return new StubIdList(result, size);
    }
  }

  @Override
  <K> void serializeIndexValue(@NotNull DataOutput out, @NotNull StubIndexKey<K, ?> stubIndexKey, @NotNull Map<K, StubIdList> map) throws IOException {
    UpdatableIndex<K, Void, FileContent> index = getIndex(stubIndexKey);
    if (index == null) return;
    KeyDescriptor<K> keyDescriptor = index.getExtension().getKeyDescriptor();

    BufferExposingByteArrayOutputStream indexOs = new BufferExposingByteArrayOutputStream();
    DataOutputStream indexDos = new DataOutputStream(indexOs);
    for (K key : map.keySet()) {
      keyDescriptor.save(indexDos, key);
      StubIdExternalizer.INSTANCE.save(indexDos, map.get(key));
    }
    DataInputOutputUtil.writeINT(out, indexDos.size());
    out.write(indexOs.getInternalBuffer(), 0, indexOs.size());
  }

  @Override
  @NotNull
  <K> Map<K, StubIdList> deserializeIndexValue(@NotNull DataInput in, @NotNull StubIndexKey<K, ?> stubIndexKey, @Nullable K requestedKey) throws IOException {
    UpdatableIndex<K, Void, FileContent> index = getIndex(stubIndexKey);
    KeyDescriptor<K> keyDescriptor = index.getExtension().getKeyDescriptor();

    int bufferSize = DataInputOutputUtil.readINT(in);
    byte[] buffer = new byte[bufferSize];
    in.readFully(buffer);
    UnsyncByteArrayInputStream indexIs = new UnsyncByteArrayInputStream(buffer);
    DataInputStream indexDis = new DataInputStream(indexIs);
    TObjectHashingStrategy<K> hashingStrategy = getKeyHashingStrategy(stubIndexKey);
    Map<K, StubIdList> result = new THashMap<>(hashingStrategy);
    while (indexDis.available() > 0) {
      K key = keyDescriptor.read(indexDis);
      StubIdList read = StubIdExternalizer.INSTANCE.read(indexDis);
      if (requestedKey != null) {
        if (hashingStrategy.equals(requestedKey, key)) {
          result.put(key, read);
          return result;
        }
      } else {
        result.put(key, read);
      }
    }
    return result;
  }

  @Override
  public <Key, Psi extends PsiElement> boolean processElements(@NotNull final StubIndexKey<Key, Psi> indexKey,
                                                               @NotNull final Key key,
                                                               @NotNull final Project project,
                                                               @Nullable final GlobalSearchScope scope,
                                                               @Nullable IdFilter idFilter,
                                                               @NotNull final Class<Psi> requiredClass,
                                                               @NotNull final Processor<? super Psi> processor) {
    boolean dumb = DumbService.isDumb(project);
    if (dumb) {
      DumbModeAccessType accessType = FileBasedIndex.getInstance().getCurrentDumbModeAccessType();
      if (accessType == DumbModeAccessType.RAW_INDEX_DATA_ACCEPTABLE) {
        throw new AssertionError("raw index data access is not available for StubIndex");
      }
    }

    IdIterator ids = getContainingIds(indexKey, key, project, idFilter, scope);
    PersistentFS fs = (PersistentFS)ManagingFS.getInstance();
    IntPredicate accessibleFileFilter = ((FileBasedIndexImpl)FileBasedIndex.getInstance()).getAccessibleFileIdFilter(project);
    // already ensured up-to-date in getContainingIds() method
    try {
      while (ids.hasNext()) {
        int id = ids.next();
        ProgressManager.checkCanceled();
        if (!accessibleFileFilter.test(id)) {
          continue;
        }
        VirtualFile file = IndexInfrastructure.findFileByIdIfCached(fs, id);
        if (file == null || (scope != null && !scope.contains(file))) {
          continue;
        }

        StubIdList list = myCachedStubIds.get(indexKey).getValue().computeIfAbsent(new CompositeKey<>(key, id), __ -> {
          return myStubProcessingHelper.retrieveStubIdList(indexKey, key, file, project);
        });
        if (list == null) {
          LOG.error("StubUpdatingIndex & " + indexKey + " stub index mismatch. No stub index key is present");
          continue;
        }
        if (!myStubProcessingHelper.processStubsInFile(project, file, list, processor, scope, requiredClass)) {
          return false;
        }
      }
    }
    catch (RuntimeException e) {
      final Throwable cause = FileBasedIndexImpl.getCauseToRebuildIndex(e);
      if (cause != null) {
        forceRebuild(cause);
      }
      else {
        throw e;
      }
    } finally {
      wipeProblematicFileIdsForParticularKeyAndStubIndex(indexKey, key);
    }
    return true;
  }

  @SuppressWarnings("unchecked")
  private <Key> UpdatableIndex<Key, Void, FileContent> getIndex(@NotNull StubIndexKey<Key, ?> indexKey) {
    return (UpdatableIndex<Key, Void, FileContent>)getAsyncState().myIndices.get(indexKey);
  }

  // Self repair for IDEA-181227, caused by (yet) unknown file event processing problem in indices
  // FileBasedIndex.requestReindex doesn't handle the situation properly because update requires old data that was lost
  private <Key> void wipeProblematicFileIdsForParticularKeyAndStubIndex(@NotNull StubIndexKey<Key, ?> indexKey,
                                                                        @NotNull Key key) {
    Set<VirtualFile> filesWithProblems = myStubProcessingHelper.takeAccumulatedFilesWithIndexProblems();

    if (filesWithProblems != null) {
      LOG.info("data for " + indexKey.getName() + " will be wiped for a some files because of internal stub processing error");
      ((FileBasedIndexImpl)FileBasedIndex.getInstance()).runCleanupAction(() -> {
        Lock writeLock = getIndex(indexKey).getWriteLock();
        boolean locked = writeLock.tryLock();
        if (!locked) return; // nested indices invocation, can not cleanup without deadlock
        try {
          for (VirtualFile file : filesWithProblems) {
            updateIndex(indexKey,
                        FileBasedIndex.getFileId(file),
                        Collections.singletonMap(key, new StubIdList()),
                        Collections.emptyMap());
          }
        }
        finally {
          writeLock.unlock();
        }
      });
    }
  }

  @Override
  public void forceRebuild(@NotNull Throwable e) {
    FileBasedIndex.getInstance().scheduleRebuild(StubUpdatingIndex.INDEX_ID, e);
  }

  @Override
  void ensureLoaded() {
    ProgressManager.getInstance().executeNonCancelableSection(() -> {
      getAsyncState();
    });
  }

  private static void requestRebuild() {
    FileBasedIndex.getInstance().requestRebuild(StubUpdatingIndex.INDEX_ID);
  }

  @Override
  @NotNull
  public <K> Collection<K> getAllKeys(@SuppressWarnings("BoundedWildcard") @NotNull StubIndexKey<K, ?> indexKey, @NotNull Project project) {
    Set<K> allKeys = new THashSet<>();
    processAllKeys(indexKey, project, Processors.cancelableCollectProcessor(allKeys));
    return allKeys;
  }

  @Override
  public <K> boolean processAllKeys(@NotNull StubIndexKey<K, ?> indexKey,
                                    @NotNull Processor<? super K> processor,
                                    @NotNull GlobalSearchScope scope,
                                    @Nullable IdFilter idFilter) {
    final UpdatableIndex<K, Void, FileContent> index = getIndex(indexKey); // wait for initialization to finish
    if (index == null ||
        !((FileBasedIndexEx)FileBasedIndex.getInstance()).ensureUpToDate(StubUpdatingIndex.INDEX_ID, scope.getProject(), scope, null)) {
      return true;
    }

    try {
      return myAccessValidator.validate(StubUpdatingIndex.INDEX_ID, ()->FileBasedIndexImpl.disableUpToDateCheckIn(()->
        index.processAllKeys(processor, scope, idFilter)));
    }
    catch (StorageException e) {
      forceRebuild(e);
    }
    catch (RuntimeException e) {
      final Throwable cause = e.getCause();
      if (cause instanceof IOException || cause instanceof StorageException) {
        forceRebuild(e);
      }
      throw e;
    }
    return true;
  }

  @NotNull
  @Override
  public <Key> IdIterator getContainingIds(@NotNull StubIndexKey<Key, ?> indexKey,
                                           @NotNull Key dataKey,
                                           @NotNull final Project project,
                                           @Nullable final GlobalSearchScope scope) {
    return getContainingIds(indexKey, dataKey, project, null, scope);
  }

  @NotNull
  private <Key> IdIterator getContainingIds(@NotNull StubIndexKey<Key, ?> indexKey,
                                           @NotNull Key dataKey,
                                           @NotNull final Project project,
                                           @Nullable IdFilter idFilter,
                                           @Nullable final GlobalSearchScope scope) {
    final FileBasedIndexImpl fileBasedIndex = (FileBasedIndexImpl)FileBasedIndex.getInstance();
    ID<Integer, SerializedStubTree> stubUpdatingIndexId = StubUpdatingIndex.INDEX_ID;
    final UpdatableIndex<Key, Void, FileContent> index = getIndex(indexKey);   // wait for initialization to finish
    if (index == null || !fileBasedIndex.ensureUpToDate(stubUpdatingIndexId, project, scope, null)) return IdIterator.EMPTY;

    if (idFilter == null) {
      idFilter = ((FileBasedIndexImpl)FileBasedIndex.getInstance()).projectIndexableFiles(project);
    }

    UpdatableIndex<Integer, SerializedStubTree, FileContent> stubUpdatingIndex = fileBasedIndex.getIndex(stubUpdatingIndexId);

    try {
      final TIntArrayList result = new TIntArrayList();
      IdFilter finalIdFilter = idFilter;
      myAccessValidator.validate(stubUpdatingIndexId, ()-> {
        // disable up-to-date check to avoid locks on attempt to acquire index write lock while holding at the same time the readLock for this index
        return FileBasedIndexImpl.disableUpToDateCheckIn(() ->
                                                           ConcurrencyUtil.withLock(stubUpdatingIndex.getReadLock(), () -> {
                                                             return index.getData(dataKey).forEach((id, value) -> {
                                                               if (finalIdFilter == null || finalIdFilter.containsFileId(id)) {
                                                                 result.add(id);
                                                               }
                                                               return true;
                                                             });
                                                           }));
      });
      return new IdIterator() {
        int cursor;

        @Override
        public boolean hasNext() {
          return cursor < result.size();
        }

        @Override
        public int next() {
          return result.get(cursor++);
        }

        @Override
        public int size() {
          return result.size();
        }
      };
    }
    catch (StorageException e) {
      forceRebuild(e);
    }
    catch (RuntimeException e) {
      final Throwable cause = FileBasedIndexImpl.getCauseToRebuildIndex(e);
      if (cause != null) {
        forceRebuild(cause);
      }
      else {
        throw e;
      }
    }

    return IdIterator.EMPTY;
  }

  void initializeStubIndexes() {
    assert !myInitialized;
    // ensure that FileBasedIndex task "FileIndexDataInitialization" submitted first
    FileBasedIndex.getInstance();
    myStateFuture = new CompletableFuture<>();
    Future<AsyncState> future = IndexInfrastructure.submitGenesisTask(new StubIndexInitialization());

    if (!IndexInfrastructure.ourDoAsyncIndicesInitialization) {
      try {
        future.get();
      }
      catch (Throwable t) {
        LOG.error(t);
      }
    }
  }

  public void dispose() {
    try {
      for (UpdatableIndex<?, ?, ?> index : getAsyncState().myIndices.values()) {
        index.dispose();
      }
    } finally {
      clearState();
    }
  }

  private void clearState() {
    myCachedStubIds.clear();
    myStateFuture = null;
    myState = null;
    myInitialized = false;
    LOG.info("StubIndexExtension-s were unloaded");
  }

  void setDataBufferingEnabled(final boolean enabled) {
    for (UpdatableIndex<?, ?, ?> index : getAsyncState().myIndices.values()) {
      index.setBufferingEnabled(enabled);
    }
  }

  void cleanupMemoryStorage() {
    UpdatableIndex<Integer, SerializedStubTree, FileContent> stubUpdatingIndex = getStubUpdatingIndex();
    stubUpdatingIndex.getWriteLock().lock();

    try {
      for (UpdatableIndex<?, ?, ?> index : getAsyncState().myIndices.values()) {
        index.cleanupMemoryStorage();
      }
    }
    finally {
      stubUpdatingIndex.getWriteLock().unlock();
    }
  }

  void clearAllIndices() {
    if (!myInitialized) return;
    for (UpdatableIndex<?, ?, ?> index : getAsyncState().myIndices.values()) {
      try {
        index.clear();
      }
      catch (StorageException e) {
        LOG.error(e);
        throw new RuntimeException(e);
      }
    }
  }

  <K> void removeTransientDataForFile(@NotNull StubIndexKey<K, ?> key, int inputId, @NotNull Collection<? extends K> keys) {
    UpdatableIndex<K, Void, FileContent> index = getIndex(key);
    index.removeTransientDataForKeys(inputId, keys);
  }

  private boolean dropUnregisteredIndices(@NotNull AsyncState state) {
    if (ApplicationManager.getApplication().isDisposed() || !IndexInfrastructure.hasIndices()) {
      return false;
    }

    Set<String> indicesToDrop = new HashSet<>(myPreviouslyRegistered != null ? myPreviouslyRegistered.registeredIndices : Collections.emptyList());
    for (ID<?, ?> key : state.myIndices.keySet()) {
      indicesToDrop.remove(key.getName());
    }

    if (!indicesToDrop.isEmpty()) {
      LOG.info("Dropping indices:" + StringUtil.join(indicesToDrop, ","));

      for (String s : indicesToDrop) {
        FileUtil.delete(IndexInfrastructure.getStubIndexRootDir(s));
      }
      return true;
    }
    return false;
  }

  @Override
  public StubIndexState getState() {
    if (!myInitialized) return null;
    return new StubIndexState(getAsyncState().myIndices.keySet());
  }

  @Override
  public void loadState(@NotNull final StubIndexState state) {
    myPreviouslyRegistered = state;
  }

  public <K> void updateIndex(@NotNull StubIndexKey<K, ?> key,
                              int fileId,
                              @NotNull final Map<K, StubIdList> oldInputData,
                              @NotNull final Map<K, StubIdList> newInputData) {
    try {
      if (FileBasedIndexImpl.DO_TRACE_STUB_INDEX_UPDATE) {
        LOG.info("stub index '" + key + "' update: " + fileId +
                 " old = " + Arrays.toString(oldInputData.keySet().toArray()) +
                 " new  = " + Arrays.toString(newInputData.keySet().toArray()) +
                 " updated_id = " + System.identityHashCode(newInputData));
      }
      final UpdatableIndex<K, Void, FileContent> index = getIndex(key);
      if (index == null) return;
      index.updateWithMap(new AbstractUpdateData<K, Void>(fileId) {
        @Override
        protected boolean iterateKeys(@NotNull KeyValueUpdateProcessor<? super K, ? super Void> addProcessor,
                                      @NotNull KeyValueUpdateProcessor<? super K, ? super Void> updateProcessor,
                                      @NotNull RemovedKeyProcessor<? super K> removeProcessor) throws StorageException {

          if (FileBasedIndexImpl.DO_TRACE_STUB_INDEX_UPDATE) {
            LOG.info("iterating keys updated_id = " + System.identityHashCode(newInputData));
          }

          boolean modified = false;

          for (K oldKey : oldInputData.keySet()) {
            if (!newInputData.containsKey(oldKey)) {
              removeProcessor.process(oldKey, fileId);
              if (!modified) modified = true;
            }
          }

          for (K oldKey : newInputData.keySet()) {
            if (!oldInputData.containsKey(oldKey)) {
              addProcessor.process(oldKey, null, fileId);
              if (!modified) modified = true;
            }
          }

          if (FileBasedIndexImpl.DO_TRACE_STUB_INDEX_UPDATE) {
            LOG.info("keys iteration finished updated_id = " + System.identityHashCode(newInputData) + "; modified = " + modified);
          }

          return modified;
        }

        @Override
        public boolean newDataIsEmpty() {
          return newInputData.isEmpty();
        }
      });
    }
    catch (StorageException e) {
      LOG.info(e);
      requestRebuild();
    }
  }

  private class StubIndexInitialization extends IndexInfrastructure.DataInitialization<AsyncState> {
    private final AsyncState state = new AsyncState();
    private final IndicesRegistrationResult indicesRegistrationSink = new IndicesRegistrationResult();

    @Override
    protected void prepare() {
      Iterator<StubIndexExtension<?, ?>> extensionsIterator =
        IndexInfrastructure.hasIndices() ?
          ((ExtensionPointImpl<StubIndexExtension<?, ?>>)StubIndexExtension.EP_NAME.getPoint(null)).iterator() :
          Collections.emptyIterator();

      boolean forceClean = Boolean.TRUE == ourForcedClean.getAndSet(Boolean.FALSE);
      while(extensionsIterator.hasNext()) {
        StubIndexExtension<?, ?> extension = extensionsIterator.next();
        if (extension == null) break;
        extension.getKey(); // initialize stub index keys

        addNestedInitializationTask(() -> {
          registerIndexer(extension, forceClean, state, indicesRegistrationSink);
        });
      }
    }

    @Override
    protected void onThrowable(@NotNull Throwable t) {
      LOG.error(t);
    }

    @Override
    protected AsyncState finish() {
      boolean someIndicesWereDropped = dropUnregisteredIndices(state);

      StringBuilder updated = new StringBuilder();
      String updatedIndices = indicesRegistrationSink.changedIndices();
      if (!updatedIndices.isEmpty()) updated.append(updatedIndices);
      if (someIndicesWereDropped && !InvertedIndex.ARE_COMPOSITE_INDEXERS_ENABLED) updated.append(" and some indices were dropped");
      indicesRegistrationSink.logChangedAndFullyBuiltIndices(LOG, "Following stub indices will be updated:",
                                                             "Following stub indices will be built:");

      if (updated.length() > 0) {
        final Throwable e = new Throwable(updated.toString());
        // avoid direct forceRebuild as it produces dependency cycle (IDEA-105485)
        AppUIExecutor.onWriteThread(ModalityState.NON_MODAL).later().submit(() -> forceRebuild(e));
      }

      myInitialized = true;
      myStateFuture.complete(state);
      return state;
    }
  }

  static UpdatableIndex<Integer, SerializedStubTree, FileContent> getStubUpdatingIndex() {
    return ((FileBasedIndexImpl)FileBasedIndex.getInstance()).getIndex(StubUpdatingIndex.INDEX_ID);
  }

  private static class CompositeKey<K> {
    private final K key;
    private final int fileId;

    private CompositeKey(K key, int id) {
      this.key = key;
      fileId = id;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      CompositeKey<?> key1 = (CompositeKey<?>)o;
      return fileId == key1.fileId && Objects.equals(key, key1.key);
    }

    @Override
    public int hashCode() {
      return Objects.hash(key, fileId);
    }
  }

  @TestOnly
  public boolean areAllProblemsProcessedInTheCurrentThread() {
    return myStubProcessingHelper.areAllProblemsProcessedInTheCurrentThread();
  }
}
