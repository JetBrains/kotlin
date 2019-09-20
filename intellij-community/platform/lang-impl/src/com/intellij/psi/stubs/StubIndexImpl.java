// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

/*
 * @author max
 */
package com.intellij.psi.stubs;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.components.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.impl.ExtensionPointImpl;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.ManagingFS;
import com.intellij.openapi.vfs.newvfs.persistent.PersistentFS;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.stubs.provided.StubProvidedIndexExtension;
import com.intellij.util.ConcurrencyUtil;
import com.intellij.util.Processor;
import com.intellij.util.Processors;
import com.intellij.util.indexing.*;
import com.intellij.util.indexing.hash.MergedInvertedIndex;
import com.intellij.util.indexing.impl.InputDataDiffBuilder;
import com.intellij.util.indexing.impl.MapInputDataDiffBuilder;
import com.intellij.util.indexing.impl.UpdateData;
import com.intellij.util.indexing.provided.ProvidedIndexExtension;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.DataInputOutputUtil;
import com.intellij.util.io.KeyDescriptor;
import gnu.trove.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReadWriteLock;

@State(name = "FileBasedIndex", storages = {
  @Storage(value = StoragePathMacros.CACHE_FILE),
  @Storage(value = "stubIndex.xml", deprecated = true, roamingType = RoamingType.DISABLED)
})
public final class StubIndexImpl extends StubIndex implements PersistentStateComponent<StubIndexState> {
  private static final AtomicReference<Boolean> ourForcedClean = new AtomicReference<>(null);
  private static final Logger LOG = Logger.getInstance("#com.intellij.psi.stubs.StubIndexImpl");

  private static class AsyncState {
    private final Map<StubIndexKey<?, ?>, UpdatableIndex<?, StubIdList, FileContent>> myIndices = new THashMap<>();
    private final Map<StubIndexKey<?, ?>, TObjectHashingStrategy<?>> myKeyHashingStrategies = new THashMap<>();
    private final TObjectIntHashMap<ID<?, ?>> myIndexIdToVersionMap = new TObjectIntHashMap<>();
  }

  private final StubProcessingHelper myStubProcessingHelper;
  private final IndexAccessValidator myAccessValidator = new IndexAccessValidator();
  private volatile Future<AsyncState> myStateFuture;
  private volatile AsyncState myState;
  private volatile boolean myInitialized;

  private StubIndexState myPreviouslyRegistered;

  public StubIndexImpl() {
    myStubProcessingHelper = new StubProcessingHelper();
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
      try {
        myState = state = myStateFuture.get();
      }
      catch (Throwable t) {
        throw new RuntimeException(t);
      }
    }
    return state;
  }

  @NotNull
  public static <K, I> FileBasedIndexExtension<K, StubIdList> wrapStubIndexExtension(StubIndexExtension<K, ?> extension) {
    return new FileBasedIndexExtension<K, StubIdList>() {
      @NotNull
      @Override
      public ID<K, StubIdList> getName() {
        return (ID<K, StubIdList>)extension.getKey();
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
      public DataIndexer<K, StubIdList, FileContent> getIndexer() {
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
      public DataExternalizer<StubIdList> getValueExternalizer() {
        return StubIdExternalizer.INSTANCE;
      }

      @Override
      public int getVersion() {
        return extension.getVersion();
      }

      @Override
      public boolean keyIsUniqueForIndexedFile() {
        return false;
      }

      @Override
      public boolean traceKeyHashToVirtualFileMapping() {
        return extension instanceof StringStubIndexExtension && ((StringStubIndexExtension)extension).traceKeyHashToVirtualFileMapping();
      }
    };
  }

  private static <K> boolean registerIndexer(@NotNull final StubIndexExtension<K, ?> extension, final boolean forceClean, @NotNull AsyncState state)
    throws IOException {
    final StubIndexKey<K, ?> indexKey = extension.getKey();
    final int version = extension.getVersion();
    FileBasedIndexExtension<K, StubIdList> wrappedExtension = wrapStubIndexExtension(extension);
    synchronized (state) {
      state.myIndexIdToVersionMap.put(indexKey, version);
    }

    final File indexRootDir = IndexInfrastructure.getIndexRootDir(indexKey);
    boolean needRebuild = false;

    if (forceClean || IndexingStamp.versionDiffers(indexKey, version)) {
      final File versionFile = IndexInfrastructure.getVersionFile(indexKey);
      final boolean versionFileExisted = versionFile.exists();

      final String[] children = indexRootDir.list();
      // rebuild only if there exists what to rebuild
      boolean indexRootHasChildren = children != null && children.length > 0;
      needRebuild = !forceClean && (versionFileExisted || indexRootHasChildren);
      if (needRebuild) {
        LOG.info("Version has changed for stub index " + extension.getKey() + ". The index will be rebuilt.");
      } else {
        LOG.debug("Stub index " + indexKey + " will be built.");
      }
      if (indexRootHasChildren) FileUtil.deleteWithRenaming(indexRootDir);
      IndexingStamp.rewriteVersion(indexKey, version); // todo snapshots indices
    }
    FileBasedIndexImpl fileBasedIndexManager = (FileBasedIndexImpl)FileBasedIndex.getInstance();
    UpdatableIndex<Integer, SerializedStubTree, FileContent> stubUpdatingIndex =
      fileBasedIndexManager.getIndex(StubUpdatingIndex.INDEX_ID);
    ReadWriteLock lock = stubUpdatingIndex.getLock();

    for (int attempt = 0; attempt < 2; attempt++) {
      try {
        final VfsAwareMapIndexStorage<K, StubIdList> storage = new VfsAwareMapIndexStorage<>(
          IndexInfrastructure.getStorageFile(indexKey),
          wrappedExtension.getKeyDescriptor(),
          wrappedExtension.getValueExternalizer(),
          wrappedExtension.getCacheSize(),
          wrappedExtension.keyIsUniqueForIndexedFile(),
          wrappedExtension.traceKeyHashToVirtualFileMapping()
        );

        final MemoryIndexStorage<K, StubIdList> memStorage = new MemoryIndexStorage<>(storage, indexKey);
        UpdatableIndex<K, StubIdList, FileContent> index = new VfsAwareMapReduceIndex<>(wrappedExtension, memStorage, null, null, null, lock);

        if (stubUpdatingIndex instanceof MergedInvertedIndex) {
          ProvidedIndexExtension<Integer, SerializedStubTree> ex =
            ((MergedInvertedIndex<Integer, SerializedStubTree>)stubUpdatingIndex).getProvidedExtension();
          if (ex instanceof StubProvidedIndexExtension) {
            ProvidedIndexExtension<K, StubIdList> providedStubIndexExtension =
              ((StubProvidedIndexExtension)ex).findProvidedStubIndex(extension);
            if (providedStubIndexExtension != null) {
              index = ProvidedIndexExtension.wrapWithProvidedIndex(providedStubIndexExtension, wrappedExtension, index);
            }
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
        needRebuild = true;
        onExceptionInstantiatingIndex(indexKey, version, indexRootDir, e);
      }
      catch (RuntimeException e) {
        Throwable cause = FileBasedIndexImpl.getCauseToRebuildIndex(e);
        if (cause == null) throw e;
        onExceptionInstantiatingIndex(indexKey, version, indexRootDir, e);
      }
    }
    return needRebuild;
  }

  @NotNull
  <K> TObjectHashingStrategy<K> getKeyHashingStrategy(StubIndexKey<K, ?> stubIndexKey) {
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
    UpdatableIndex<?, StubIdList, FileContent> index = getAsyncState().myIndices.get(indexId);
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
    for (UpdatableIndex<?, StubIdList, FileContent> index : getAsyncState().myIndices.values()) {
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

  <K> void serializeIndexValue(@NotNull DataOutput out, @NotNull StubIndexKey<K, ?> stubIndexKey, @NotNull Map<K, StubIdList> map) throws IOException {
    UpdatableIndex<K, StubIdList, FileContent> index = getIndex(stubIndexKey);
    if (index == null) return;
    KeyDescriptor<K> keyDescriptor = index.getExtension().getKeyDescriptor();

    DataInputOutputUtil.writeINT(out, map.size());
    for (K key : map.keySet()) {
      keyDescriptor.save(out, key);
      StubIdExternalizer.INSTANCE.save(out, map.get(key));
    }
  }

  @NotNull
  <K> Map<K, StubIdList> deserializeIndexValue(@NotNull DataInput in, @NotNull StubIndexKey<K, ?> stubIndexKey) throws IOException {
    UpdatableIndex<K, StubIdList, FileContent> index = getIndex(stubIndexKey);
    KeyDescriptor<K> keyDescriptor = index.getExtension().getKeyDescriptor();
    int mapSize = DataInputOutputUtil.readINT(in);

    Map<K, StubIdList> result = new THashMap<>(mapSize, getKeyHashingStrategy(stubIndexKey));
    for (int i = 0; i < mapSize; ++i) {
      K key = keyDescriptor.read(in);
      StubIdList read = StubIdExternalizer.INSTANCE.read(in);
      result.put(key, read);
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
    return doProcessStubs(indexKey, key, project, scope, new StubIdListContainerAction(idFilter, project) {
      final PersistentFS fs = (PersistentFS)ManagingFS.getInstance();

      @Override
      protected boolean process(int id, @NotNull StubIdList value) {
        final VirtualFile file = IndexInfrastructure.findFileByIdIfCached(fs, id);
        if (file == null || scope != null && !scope.contains(file)) {
          return true;
        }
        return myStubProcessingHelper.processStubsInFile(project, file, value, processor, scope, requiredClass);
      }
    });
  }

  private <Key> boolean doProcessStubs(@NotNull final StubIndexKey<Key, ?> indexKey,
                                       @NotNull final Key key,
                                       @NotNull final Project project,
                                       @Nullable final GlobalSearchScope scope,
                                       @NotNull StubIdListContainerAction action) {
    final FileBasedIndexImpl fileBasedIndex = (FileBasedIndexImpl)FileBasedIndex.getInstance();
    ID<Integer, SerializedStubTree> stubUpdatingIndexId = StubUpdatingIndex.INDEX_ID;
    final UpdatableIndex<Key, StubIdList, FileContent> index = getIndex(indexKey);   // wait for initialization to finish
    if (index == null) return true;

    fileBasedIndex.ensureUpToDate(stubUpdatingIndexId, project, scope);

    UpdatableIndex<Integer, SerializedStubTree, FileContent> stubUpdatingIndex = fileBasedIndex.getIndex(stubUpdatingIndexId);
    try {
      return myAccessValidator.validate(stubUpdatingIndexId, ()-> {
        try {
          return FileBasedIndexImpl.disableUpToDateCheckIn(() ->
             ConcurrencyUtil.withLock(stubUpdatingIndex.getReadLock(), () ->
               // disable up-to-date check to avoid locks on attempt to acquire index write lock while holding at the same time the readLock for this index
               index.getData(key).forEach(action)
             ));
        }
        finally {
          wipeProblematicFileIdsForParticularKeyAndStubIndex(indexKey, key, stubUpdatingIndex);
        }
      });
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

    return true;
  }

  @SuppressWarnings("unchecked")
  private <Key> UpdatableIndex<Key, StubIdList, FileContent> getIndex(@NotNull StubIndexKey<Key, ?> indexKey) {
    return (UpdatableIndex<Key, StubIdList, FileContent>)getAsyncState().myIndices.get(indexKey);
  }

  // Self repair for IDEA-181227, caused by (yet) unknown file event processing problem in indices
  // FileBasedIndex.requestReindex doesn't handle the situation properly because update requires old data that was lost
  private <Key> void wipeProblematicFileIdsForParticularKeyAndStubIndex(@NotNull StubIndexKey<Key, ?> indexKey,
                                                                        @NotNull Key key,
                                                                        @NotNull UpdatableIndex<Integer, SerializedStubTree, FileContent> stubUpdatingIndex) {
    Set<VirtualFile> filesWithProblems = myStubProcessingHelper.takeAccumulatedFilesWithIndexProblems();

    if (filesWithProblems != null) {
      ((FileBasedIndexImpl)FileBasedIndex.getInstance()).runCleanupAction(() -> {
        boolean locked = stubUpdatingIndex.getWriteLock().tryLock();
        if (!locked) return; // nested indices invocation, can not cleanup without deadlock
        try {
          Map<Key, StubIdList> artificialOldValues = new THashMap<>();
          artificialOldValues.put(key, new StubIdList());

          for (VirtualFile file : filesWithProblems) {
            updateIndex(indexKey, FileBasedIndex.getFileId(file), artificialOldValues, Collections.emptyMap());
          }
        }
        finally {
          stubUpdatingIndex.getWriteLock().unlock();
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
  @NotNull
  public <K> Collection<K> getAllKeys(@NotNull StubIndexKey<K, ?> indexKey, @NotNull Project project) {
    Set<K> allKeys = new THashSet<>();
    processAllKeys(indexKey, project, Processors.cancelableCollectProcessor(allKeys));
    return allKeys;
  }

  @Override
  public <K> boolean processAllKeys(@NotNull StubIndexKey<K, ?> indexKey,
                                    @NotNull Processor<? super K> processor,
                                    @NotNull GlobalSearchScope scope,
                                    @Nullable IdFilter idFilter) {
    final UpdatableIndex<K, StubIdList, FileContent> index = getIndex(indexKey); // wait for initialization to finish
    if (index == null) return true;
    FileBasedIndex.getInstance().ensureUpToDate(StubUpdatingIndex.INDEX_ID, scope.getProject(), scope);

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
                                           @NotNull final GlobalSearchScope scope) {
    final TIntArrayList result = new TIntArrayList();
    doProcessStubs(indexKey, dataKey, project, scope, new StubIdListContainerAction(null, project) {
      @Override
      protected boolean process(int id, @NotNull StubIdList value) {
        result.add(id);
        return true;
      }
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

  @Override
  public void initializeComponent() {
    // ensure that FileBasedIndex task "FileIndexDataInitialization" submitted first
    FileBasedIndex.getInstance();
    myStateFuture = IndexInfrastructure.submitGenesisTask(new StubIndexInitialization());

    if (!IndexInfrastructure.ourDoAsyncIndicesInitialization) {
      try {
        myStateFuture.get();
      }
      catch (Throwable t) {
        LOG.error(t);
      }
    }
  }

  static void initExtensions() {
    // initialize stub index keys
    for (StubIndexExtension extension : StubIndexExtension.EP_NAME.getExtensionList()) {
      extension.getKey();
    }
  }

  public void dispose() {
    for (UpdatableIndex index : getAsyncState().myIndices.values()) {
      index.dispose();
    }
  }

  void setDataBufferingEnabled(final boolean enabled) {
    for (UpdatableIndex index : getAsyncState().myIndices.values()) {
      index.setBufferingEnabled(enabled);
    }
  }

  void cleanupMemoryStorage() {
    UpdatableIndex<Integer, SerializedStubTree, FileContent> stubUpdatingIndex =
      ((FileBasedIndexImpl)FileBasedIndex.getInstance()).getIndex(StubUpdatingIndex.INDEX_ID);
    stubUpdatingIndex.getWriteLock().lock();

    try {
      for (UpdatableIndex index : getAsyncState().myIndices.values()) {
        index.cleanupMemoryStorage();
      }
    }
    finally {
      stubUpdatingIndex.getWriteLock().unlock();
    }
  }

  void clearAllIndices() {
    if (!myInitialized) return;
    for (UpdatableIndex index : getAsyncState().myIndices.values()) {
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
    UpdatableIndex<K, StubIdList, FileContent> index = getIndex(key);
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
        FileUtil.delete(IndexInfrastructure.getIndexRootDir(StubIndexKey.createIndexKey(s)));
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

  public <K> void updateIndex(@NotNull StubIndexKey key,
                              int fileId,
                              @NotNull final Map<K, StubIdList> oldValues,
                              @NotNull final Map<K, StubIdList> newValues) {
    try {
      final UpdatableIndex<K, StubIdList, FileContent> index = getIndex((StubIndexKey<K, ?>)key);
      if (index == null) return;
      final ThrowableComputable<InputDataDiffBuilder<K, StubIdList>, IOException>
        oldMapGetter = () -> new MapInputDataDiffBuilder<>(fileId, oldValues);
      index.updateWithMap(new UpdateData<>(fileId, newValues, oldMapGetter, key, null));
    }
    catch (StorageException e) {
      LOG.info(e);
      requestRebuild();
    }
  }

  private abstract static class StubIdListContainerAction implements ValueContainer.ContainerAction<StubIdList> {
    private final IdFilter myIdFilter;

    StubIdListContainerAction(@Nullable IdFilter idFilter, @NotNull Project project) {
      myIdFilter = idFilter != null ? idFilter : ((FileBasedIndexImpl)FileBasedIndex.getInstance()).projectIndexableFiles(project);
    }

    @Override
    public boolean perform(final int id, @NotNull final StubIdList value) {
      ProgressManager.checkCanceled();
      if (myIdFilter != null && !myIdFilter.containsFileId(id)) return true;

      return process(id, value);
    }

    protected abstract boolean process(int id, @NotNull StubIdList value);
  }

  private class StubIndexInitialization extends IndexInfrastructure.DataInitialization<AsyncState> {
    private final AsyncState state = new AsyncState();
    private final StringBuilder updated = new StringBuilder();

    @Override
    protected void prepare() {
      Iterator<StubIndexExtension<?, ?>> extensionsIterator =
        IndexInfrastructure.hasIndices() ?
          ((ExtensionPointImpl<StubIndexExtension<?, ?>>)StubIndexExtension.EP_NAME.getPoint(null)).iterator() :
          Collections.emptyIterator();

      boolean forceClean = Boolean.TRUE == ourForcedClean.getAndSet(Boolean.FALSE);
      while(extensionsIterator.hasNext()) {
        StubIndexExtension extension = extensionsIterator.next();
        if (extension == null) break;
        extension.getKey(); // initialize stub index keys

        addNestedInitializationTask(() -> {
          @SuppressWarnings("unchecked") boolean rebuildRequested = registerIndexer(extension, forceClean, state);
          if (rebuildRequested) {
            synchronized (updated) {
              updated.append(extension).append(' ');
            }
          }
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
      if (someIndicesWereDropped) updated.append(" and some indices were dropped");

      if (updated.length() > 0) {
        final Throwable e = new Throwable(updated.toString());
        // avoid direct forceRebuild as it produces dependency cycle (IDEA-105485)
        ApplicationManager.getApplication().invokeLater(() -> forceRebuild(e), ModalityState.NON_MODAL);
      }

      myInitialized = true;
      return state;
    }
  }
}
