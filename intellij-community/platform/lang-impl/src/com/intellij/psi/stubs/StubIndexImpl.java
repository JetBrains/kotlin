// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.stubs;

import com.intellij.model.ModelBranchImpl;
import com.intellij.openapi.application.AppUIExecutor;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.ExtensionPointListener;
import com.intellij.openapi.extensions.PluginDescriptor;
import com.intellij.openapi.extensions.impl.ExtensionPointImpl;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.util.ProgressIndicatorUtils;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.ModificationTracker;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.persistent.PersistentFS;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.EverythingGlobalScope;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.util.*;
import com.intellij.util.containers.FactoryMap;
import com.intellij.util.indexing.*;
import com.intellij.util.indexing.impl.AbstractUpdateData;
import com.intellij.util.indexing.impl.KeyValueUpdateProcessor;
import com.intellij.util.indexing.impl.RemovedKeyProcessor;
import com.intellij.util.indexing.impl.storage.TransientChangesIndexStorage;
import com.intellij.util.indexing.impl.storage.VfsAwareMapIndexStorage;
import com.intellij.util.indexing.impl.storage.VfsAwareMapReduceIndex;
import com.intellij.util.indexing.memory.InMemoryIndexStorage;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.KeyDescriptor;
import com.intellij.util.io.VoidDataExternalizer;
import gnu.trove.THashMap;
import gnu.trove.THashSet;
import gnu.trove.TIntArrayList;
import gnu.trove.TObjectIntHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.function.IntPredicate;

@State(name = "FileBasedIndex", storages = @Storage(StoragePathMacros.CACHE_FILE))
public final class StubIndexImpl extends StubIndexEx implements PersistentStateComponent<StubIndexState> {
  private static final AtomicReference<Boolean> ourForcedClean = new AtomicReference<>(null);
  static final Logger LOG = Logger.getInstance(StubIndexImpl.class);

  private static final class AsyncState {
    private final Map<StubIndexKey<?, ?>, UpdatableIndex<?, Void, FileContent>> myIndices = new THashMap<>();
    private final TObjectIntHashMap<ID<?, ?>> myIndexIdToVersionMap = new TObjectIntHashMap<>();
  }

  private final Map<StubIndexKey<?, ?>, CachedValue<Map<CompositeKey<?>, StubIdList>>> myCachedStubIds = FactoryMap.createMap(k -> {
    UpdatableIndex<Integer, SerializedStubTree, FileContent> index = getStubUpdatingIndex();
    ModificationTracker tracker = index::getModificationStamp;
    return new CachedValueImpl<>(() -> new CachedValueProvider.Result<>(new ConcurrentHashMap<>(), tracker));
  }, ConcurrentHashMap::new);

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

  static @Nullable StubIndexImpl getInstanceOrInvalidate() {
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

  public static @NotNull <K> FileBasedIndexExtension<K, Void> wrapStubIndexExtension(StubIndexExtension<K, ?> extension) {
    return new FileBasedIndexExtension<K, Void>() {
      @Override
      public @NotNull ID<K, Void> getName() {
        //noinspection unchecked
        return (ID<K, Void>)extension.getKey();
      }

      @Override
      public @NotNull FileBasedIndex.InputFilter getInputFilter() {
        return f -> {
          throw new UnsupportedOperationException();
        };
      }

      @Override
      public boolean dependsOnFileContent() {
        return true;
      }

      @Override
      public boolean needsForwardIndexWhenSharing() {
        return false;
      }

      @Override
      public @NotNull DataIndexer<K, Void, FileContent> getIndexer() {
        return i -> {
          throw new AssertionError();
        };
      }

      @Override
      public @NotNull KeyDescriptor<K> getKeyDescriptor() {
        return extension.getKeyDescriptor();
      }

      @Override
      public @NotNull DataExternalizer<Void> getValueExternalizer() {
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
  private static <K> void registerIndexer(final @NotNull StubIndexExtension<K, ?> extension, final boolean forceClean,
                                          @NotNull AsyncState state, @NotNull IndexVersionRegistrationSink registrationResultSink)
    throws IOException {
    final StubIndexKey<K, ?> indexKey = extension.getKey();
    final int version = extension.getVersion();
    FileBasedIndexExtension<K, Void> wrappedExtension = wrapStubIndexExtension(extension);
    synchronized (state) {
      state.myIndexIdToVersionMap.put(indexKey, version);
    }

    final File indexRootDir = IndexInfrastructure.getIndexRootDir(indexKey);

    IndexingStamp.IndexVersionDiff versionDiff = forceClean
                                                 ? new IndexingStamp.IndexVersionDiff.InitialBuild(version)
                                                 : IndexingStamp.versionDiffers(indexKey, version);

    registrationResultSink.setIndexVersionDiff(indexKey, versionDiff);
    if (versionDiff != IndexingStamp.IndexVersionDiff.UP_TO_DATE) {
      final File versionFile = IndexInfrastructure.getVersionFile(indexKey);
      final boolean versionFileExisted = versionFile.exists();

      final String[] children = indexRootDir.list();
      // rebuild only if there exists what to rebuild
      boolean indexRootHasChildren = children != null && children.length > 0;
      boolean needRebuild = !forceClean && (versionFileExisted || indexRootHasChildren);

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
    }

    UpdatableIndex<Integer, SerializedStubTree, FileContent> stubUpdatingIndex = getStubUpdatingIndex();
    ReadWriteLock lock = stubUpdatingIndex.getLock();

    for (int attempt = 0; attempt < 2; attempt++) {
      try {
        final VfsAwareIndexStorage<K, Void> storage = FileBasedIndex.USE_IN_MEMORY_INDEX
                                                      ? new InMemoryIndexStorage<>()
                                                      : new VfsAwareMapIndexStorage<>(
          IndexInfrastructure.getStorageFile(indexKey).toPath(),
          wrappedExtension.getKeyDescriptor(),
          wrappedExtension.getValueExternalizer(),
          wrappedExtension.getCacheSize(),
          wrappedExtension.keyIsUniqueForIndexedFile(),
          wrappedExtension.traceKeyHashToVirtualFileMapping()
        );
        final TransientChangesIndexStorage<K, Void> memStorage = new TransientChangesIndexStorage<>(storage, indexKey);
        UpdatableIndex<K, Void, FileContent> index = new VfsAwareMapReduceIndex<>(wrappedExtension, memStorage, null, null, null, lock);

        for (FileBasedIndexInfrastructureExtension infrastructureExtension : FileBasedIndexInfrastructureExtension.EP_NAME.getExtensionList()) {
          UpdatableIndex<K, Void, FileContent> intermediateIndex = infrastructureExtension.combineIndex(wrappedExtension, index);
          if (intermediateIndex != null) {
            index = intermediateIndex;
          }
        }

        synchronized (state) {
          state.myIndices.put(indexKey, index);
        }
        break;
      }
      catch (IOException e) {
        registrationResultSink.setIndexVersionDiff(indexKey, new IndexingStamp.IndexVersionDiff.CorruptedRebuild(version));
        onExceptionInstantiatingIndex(indexKey, version, indexRootDir, e);
      }
      catch (RuntimeException e) {
        Throwable cause = FileBasedIndexImpl.getCauseToRebuildIndex(e);
        if (cause == null) throw e;
        onExceptionInstantiatingIndex(indexKey, version, indexRootDir, e);
      }
    }
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


  @Override
  public <Key, Psi extends PsiElement> boolean processElements(final @NotNull StubIndexKey<Key, Psi> indexKey,
                                                               final @NotNull Key key,
                                                               final @NotNull Project project,
                                                               final @Nullable GlobalSearchScope scope,
                                                               @Nullable IdFilter idFilter,
                                                               final @NotNull Class<Psi> requiredClass,
                                                               final @NotNull Processor<? super Psi> processor) {
    boolean dumb = DumbService.isDumb(project);
    if (dumb) {
      DumbModeAccessType accessType = FileBasedIndex.getInstance().getCurrentDumbModeAccessType();
      if (accessType == DumbModeAccessType.RAW_INDEX_DATA_ACCEPTABLE) {
        throw new AssertionError("raw index data access is not available for StubIndex");
      }
    }

    PairProcessor<VirtualFile, StubIdList> stubProcessor = (file, list) ->
      myStubProcessingHelper.processStubsInFile(project, file, list, processor, scope, requiredClass);

    if (!ModelBranchImpl.processBranchedFilesInScope(scope != null ? scope : new EverythingGlobalScope(project),
                                                     file -> processInMemoryStubs(indexKey, key, project, stubProcessor, file))) {
      return false;
    }

    IdIterator ids = getContainingIds(indexKey, key, project, idFilter, scope);
    PersistentFS fs = PersistentFS.getInstance();
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

        StubIdList list = myCachedStubIds.get(indexKey).getValue().computeIfAbsent(new CompositeKey<>(key, id), __ ->
          myStubProcessingHelper.retrieveStubIdList(indexKey, key, file, project)
        );
        if (list == null) {
          LOG.error("StubUpdatingIndex & " + indexKey + " stub index mismatch. No stub index key is present");
          continue;
        }
        if (!stubProcessor.process(file, list)) {
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

  private static <Key, Psi extends PsiElement> boolean processInMemoryStubs(StubIndexKey<Key, Psi> indexKey,
                                                                            Key key,
                                                                            Project project,
                                                                            PairProcessor<VirtualFile, StubIdList> stubProcessor,
                                                                            VirtualFile file) {
    Map<Integer, SerializedStubTree> data = FileBasedIndex.getInstance().getFileData(StubUpdatingIndex.INDEX_ID, file, project);
    if (data.size() == 1) {
      try {
        StubIdList list = data.values().iterator().next().restoreIndexedStubs(indexKey, key);
        if (list != null) {
          return stubProcessor.process(file, list);
        }
      }
      catch (IOException e) {
        throw new RuntimeException(e);
      }
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
        Lock writeLock = getIndex(indexKey).getLock().writeLock();
        boolean locked = writeLock.tryLock();
        if (!locked) return; // nested indices invocation, can not cleanup without deadlock
        try {
          for (VirtualFile file : filesWithProblems) {
            updateIndex(indexKey,
                        FileBasedIndex.getFileId(file),
                        Collections.singleton(key),
                        Collections.emptySet());
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

  private static void requestRebuild() {
    FileBasedIndex.getInstance().requestRebuild(StubUpdatingIndex.INDEX_ID);
  }

  @Override
  public @NotNull <K> Collection<K> getAllKeys(@SuppressWarnings("BoundedWildcard") @NotNull StubIndexKey<K, ?> indexKey, @NotNull Project project) {
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

  @Override
  public @NotNull <Key> IdIterator getContainingIds(@NotNull StubIndexKey<Key, ?> indexKey,
                                                    @NotNull Key dataKey,
                                                    final @NotNull Project project,
                                                    final @Nullable GlobalSearchScope scope) {
    return getContainingIds(indexKey, dataKey, project, null, scope);
  }

  private @NotNull <Key> IdIterator getContainingIds(@NotNull StubIndexKey<Key, ?> indexKey,
                                                     @NotNull Key dataKey,
                                                     final @NotNull Project project,
                                                     @Nullable IdFilter idFilter,
                                                     final @Nullable GlobalSearchScope scope) {
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
        return FileBasedIndexImpl.disableUpToDateCheckIn(() -> ConcurrencyUtil.withLock(stubUpdatingIndex.getLock().readLock(), () ->
          index.getData(dataKey).forEach((id, value) -> {
            if (finalIdFilter == null || finalIdFilter.containsFileId(id)) {
              result.add(id);
            }
            return true;
          })
        ));
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
    StubIndexKeyDescriptorCache.INSTANCE.clear();
    ((SerializationManagerImpl)SerializationManager.getInstance()).dropSerializerData();
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
    stubUpdatingIndex.getLock().writeLock().lock();

    try {
      for (UpdatableIndex<?, ?, ?> index : getAsyncState().myIndices.values()) {
        index.cleanupMemoryStorage();
      }
    }
    finally {
      stubUpdatingIndex.getLock().writeLock().unlock();
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

  private void dropUnregisteredIndices(@NotNull AsyncState state) {
    if (ApplicationManager.getApplication().isDisposed() || !IndexInfrastructure.hasIndices()) {
      return;
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
    }
  }

  @Override
  public StubIndexState getState() {
    if (!myInitialized) return null;
    return new StubIndexState(getAsyncState().myIndices.keySet());
  }

  @Override
  public void loadState(final @NotNull StubIndexState state) {
    myPreviouslyRegistered = state;
  }

  public <K> void updateIndex(@NotNull StubIndexKey<K, ?> stubIndexKey,
                              int fileId,
                              @NotNull Set<K> oldKeys,
                              @NotNull Set<K> newKeys) {
    ProgressManager.getInstance().executeNonCancelableSection(() -> {
      try {
        if (FileBasedIndexImpl.DO_TRACE_STUB_INDEX_UPDATE) {
          LOG.info("stub index '" + stubIndexKey + "' update: " + fileId +
                   " old = " + Arrays.toString(oldKeys.toArray()) +
                   " new  = " + Arrays.toString(newKeys.toArray()) +
                   " updated_id = " + System.identityHashCode(newKeys));
        }
        final UpdatableIndex<K, Void, FileContent> index = getIndex(stubIndexKey);
        if (index == null) return;
        index.updateWithMap(new AbstractUpdateData<K, Void>(fileId) {
          @Override
          protected boolean iterateKeys(@NotNull KeyValueUpdateProcessor<? super K, ? super Void> addProcessor,
                                        @NotNull KeyValueUpdateProcessor<? super K, ? super Void> updateProcessor,
                                        @NotNull RemovedKeyProcessor<? super K> removeProcessor) throws StorageException {

            if (FileBasedIndexImpl.DO_TRACE_STUB_INDEX_UPDATE) {
              LOG.info("iterating keys updated_id = " + System.identityHashCode(newKeys));
            }

            boolean modified = false;

            for (K oldKey : oldKeys) {
              if (!newKeys.contains(oldKey)) {
                removeProcessor.process(oldKey, fileId);
                if (!modified) modified = true;
              }
            }

            for (K oldKey : newKeys) {
              if (!oldKeys.contains(oldKey)) {
                addProcessor.process(oldKey, null, fileId);
                if (!modified) modified = true;
              }
            }

            if (FileBasedIndexImpl.DO_TRACE_STUB_INDEX_UPDATE) {
              LOG.info("keys iteration finished updated_id = " + System.identityHashCode(newKeys) + "; modified = " + modified);
            }

            return modified;
          }
        });
      }
      catch (StorageException e) {
        LOG.info(e);
        requestRebuild();
      }
    });

  }

  private class StubIndexInitialization extends IndexInfrastructure.DataInitialization<AsyncState> {
    private final AsyncState state = new AsyncState();
    private final IndexVersionRegistrationSink indicesRegistrationSink = new IndexVersionRegistrationSink();

    @Override
    protected void prepare() {
      Iterator<StubIndexExtension<?, ?>> extensionsIterator =
        IndexInfrastructure.hasIndices() ?
          ((ExtensionPointImpl<StubIndexExtension<?, ?>>)StubIndexExtension.EP_NAME.getPoint()).iterator() :
          Collections.emptyIterator();

      boolean forceClean = Boolean.TRUE == ourForcedClean.getAndSet(Boolean.FALSE);
      while(extensionsIterator.hasNext()) {
        StubIndexExtension<?, ?> extension = extensionsIterator.next();
        if (extension == null) break;
        extension.getKey(); // initialize stub index keys

        addNestedInitializationTask(() -> registerIndexer(extension, forceClean, state, indicesRegistrationSink));
      }
    }

    @Override
    protected void onThrowable(@NotNull Throwable t) {
      LOG.error(t);
    }

    @Override
    protected AsyncState finish() {
      dropUnregisteredIndices(state);

      indicesRegistrationSink.logChangedAndFullyBuiltIndices(LOG, "Following stub indices will be updated:",
                                                             "Following stub indices will be built:");

      if (indicesRegistrationSink.hasChangedIndexes()) {
        final Throwable e = new Throwable(indicesRegistrationSink.changedIndices());
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

  private static final class CompositeKey<K> {
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
