// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.util.indexing;

import com.google.common.annotations.VisibleForTesting;
import com.intellij.AppTopics;
import com.intellij.history.LocalHistory;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.ide.startup.ServiceNotReadyException;
import com.intellij.lang.ASTNode;
import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.application.*;
import com.intellij.openapi.application.impl.LaterInvocator;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.impl.EditorHighlighterCache;
import com.intellij.openapi.extensions.impl.ExtensionPointImpl;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileDocumentManagerListener;
import com.intellij.openapi.fileTypes.*;
import com.intellij.openapi.fileTypes.impl.FileTypeManagerImpl;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator;
import com.intellij.openapi.progress.util.ProgressIndicatorUtils;
import com.intellij.openapi.project.*;
import com.intellij.openapi.roots.CollectingContentIterator;
import com.intellij.openapi.roots.ContentIterator;
import com.intellij.openapi.roots.impl.PushedFilePropertiesUpdaterImpl;
import com.intellij.openapi.util.*;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.*;
import com.intellij.openapi.vfs.newvfs.ManagingFS;
import com.intellij.openapi.vfs.newvfs.NewVirtualFile;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.openapi.vfs.newvfs.impl.VirtualFileSystemEntry;
import com.intellij.openapi.vfs.newvfs.persistent.FlushingDaemon;
import com.intellij.openapi.vfs.newvfs.persistent.PersistentFS;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.SingleRootFileViewProvider;
import com.intellij.psi.impl.PsiDocumentTransactionListener;
import com.intellij.psi.impl.PsiManagerImpl;
import com.intellij.psi.impl.PsiTreeChangeEventImpl;
import com.intellij.psi.impl.cache.impl.id.IdIndex;
import com.intellij.psi.impl.cache.impl.id.PlatformIdTableBuilding;
import com.intellij.psi.impl.source.PsiFileImpl;
import com.intellij.psi.search.EverythingGlobalScope;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.stubs.SerializationManagerEx;
import com.intellij.util.*;
import com.intellij.util.concurrency.BoundedTaskExecutor;
import com.intellij.util.concurrency.SequentialTaskExecutor;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.IntObjectMap;
import com.intellij.util.gist.GistManager;
import com.intellij.util.indexing.impl.InvertedIndexValueIterator;
import com.intellij.util.indexing.impl.MapReduceIndex;
import com.intellij.util.io.DataOutputStream;
import com.intellij.util.io.IOUtil;
import com.intellij.util.io.storage.HeavyProcessLatch;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.util.ui.UIUtil;
import gnu.trove.THashMap;
import gnu.trove.THashSet;
import gnu.trove.TIntArrayList;
import gnu.trove.TIntHashSet;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.io.*;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * @author Eugene Zhuravlev
 */
public final class FileBasedIndexImpl extends FileBasedIndex implements Disposable {
  private static final ThreadLocal<VirtualFile> ourIndexedFile = new ThreadLocal<>();
  static final Logger LOG = Logger.getInstance("#com.intellij.util.indexing.FileBasedIndexImpl");
  private static final String CORRUPTION_MARKER_NAME = "corruption.marker";
  private static final NotificationGroup NOTIFICATIONS = new NotificationGroup("Indexing", NotificationDisplayType.BALLOON, false);

  private final List<ID<?, ?>> myIndicesForDirectories = new SmartList<>();

  private final Map<ID<?, ?>, DocumentUpdateTask> myUnsavedDataUpdateTasks = new ConcurrentHashMap<>();

  private final Set<ID<?, ?>> myNotRequiringContentIndices = new THashSet<>();
  private final Set<ID<?, ?>> myRequiringContentIndices = new THashSet<>();
  private final Set<ID<?, ?>> myPsiDependentIndices = new THashSet<>();
  private final Set<FileType> myNoLimitCheckTypes = new THashSet<>();

  private volatile boolean myExtensionsRelatedDataWasLoaded;

  private final PerIndexDocumentVersionMap myLastIndexedDocStamps = new PerIndexDocumentVersionMap();
  @NotNull private final ChangedFilesCollector myChangedFilesCollector;

  private final List<IndexableFileSet> myIndexableSets = ContainerUtil.createLockFreeCopyOnWriteList();
  private final Map<IndexableFileSet, Project> myIndexableSetToProjectMap = new THashMap<>();

  private final MessageBusConnection myConnection;
  private final FileDocumentManager myFileDocumentManager;
  private final FileTypeManagerImpl myFileTypeManager;

  private final Set<ID<?, ?>> myUpToDateIndicesForUnsavedOrTransactedDocuments = ContainerUtil.newConcurrentSet();
  private volatile SmartFMap<Document, PsiFile> myTransactionMap = SmartFMap.emptyMap();

  private final boolean myIsUnitTestMode;
  @Nullable private ScheduledFuture<?> myFlushingFuture;
  private final AtomicInteger myLocalModCount = new AtomicInteger();
  private final AtomicInteger myFilesModCount = new AtomicInteger();
  private final AtomicInteger myUpdatingFiles = new AtomicInteger();
  private final Set<Project> myProjectsBeingUpdated = ContainerUtil.newConcurrentSet();
  private final IndexAccessValidator myAccessValidator = new IndexAccessValidator();

  private volatile boolean myInitialized;

  private Future<IndexConfiguration> myStateFuture;
  private volatile IndexConfiguration myState;
  private volatile Future<?> myAllIndicesInitializedFuture;

  private IndexConfiguration getState() {
    if (!myInitialized) {
      //throw new IndexNotReadyException();
      LOG.error("Unexpected initialization problem");
    }

    IndexConfiguration state = myState; // memory barrier
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

  public FileBasedIndexImpl(@SuppressWarnings("UnusedParameters") VirtualFileManager vfManager,
                            FileDocumentManager fdm,
                            FileTypeManagerImpl fileTypeManager,
                            @NotNull MessageBus bus) {
    myFileDocumentManager = fdm;
    myFileTypeManager = fileTypeManager;
    myIsUnitTestMode = ApplicationManager.getApplication().isUnitTestMode();

    final MessageBusConnection connection = bus.connect();
    connection.subscribe(PsiDocumentTransactionListener.TOPIC, new PsiDocumentTransactionListener() {
      @Override
      public void transactionStarted(@NotNull final Document doc, @NotNull final PsiFile file) {
        myTransactionMap = myTransactionMap.plus(doc, file);
        myUpToDateIndicesForUnsavedOrTransactedDocuments.clear();
      }

      @Override
      public void transactionCompleted(@NotNull final Document doc, @NotNull final PsiFile file) {
        myTransactionMap = myTransactionMap.minus(doc);
      }
    });

    connection.subscribe(FileTypeManager.TOPIC, new FileTypeListener() {
      @Nullable private Map<FileType, Set<String>> myTypeToExtensionMap;

      @Override
      public void beforeFileTypesChanged(@NotNull final FileTypeEvent event) {
        cleanupProcessedFlag();
        myTypeToExtensionMap = new THashMap<>();
        for (FileType type : myFileTypeManager.getRegisteredFileTypes()) {
          myTypeToExtensionMap.put(type, getExtensions(type));
        }
      }

      @Override
      public void fileTypesChanged(@NotNull final FileTypeEvent event) {
        final Map<FileType, Set<String>> oldTypeToExtensionsMap = myTypeToExtensionMap;
        myTypeToExtensionMap = null;
        if (oldTypeToExtensionsMap != null) {
          final Map<FileType, Set<String>> newTypeToExtensionsMap = new THashMap<>();
          for (FileType type : myFileTypeManager.getRegisteredFileTypes()) {
            newTypeToExtensionsMap.put(type, getExtensions(type));
          }
          // we are interested only in extension changes or removals.
          // addition of an extension is handled separately by RootsChanged event
          if (!newTypeToExtensionsMap.keySet().containsAll(oldTypeToExtensionsMap.keySet())) {
            Set<FileType> removedFileTypes = new HashSet<>(oldTypeToExtensionsMap.keySet());
            removedFileTypes.removeAll(newTypeToExtensionsMap.keySet());
            rebuildAllIndices("The following file types were removed/are no longer associated: " + removedFileTypes);
            return;
          }
          for (Map.Entry<FileType, Set<String>> entry : oldTypeToExtensionsMap.entrySet()) {
            FileType fileType = entry.getKey();
            Set<String> strings = entry.getValue();
            if (!newTypeToExtensionsMap.get(fileType).containsAll(strings)) {
              Set<String> removedExtensions = new HashSet<>(strings);
              removedExtensions.removeAll(newTypeToExtensionsMap.get(fileType));
              rebuildAllIndices(fileType.getName() + " is no longer associated with extension(s) " + String.join(",", removedExtensions));
              return;
            }
          }
        }
      }

      @NotNull
      private Set<String> getExtensions(@NotNull FileType type) {
        final Set<String> set = new THashSet<>();
        for (FileNameMatcher matcher : myFileTypeManager.getAssociations(type)) {
          set.add(matcher.getPresentableString());
        }
        return set;
      }

      private void rebuildAllIndices(@NotNull String reason) {
        doClearIndices();
        scheduleIndexRebuild("File type change" + ", " + reason);
      }
    });

    connection.subscribe(AppTopics.FILE_DOCUMENT_SYNC, new FileDocumentManagerListener() {
      @Override
      public void fileContentReloaded(@NotNull VirtualFile file, @NotNull Document document) {
        cleanupMemoryStorage(true);
      }

      @Override
      public void unsavedDocumentsDropped() {
        cleanupMemoryStorage(false);
      }
    });

    ApplicationManager.getApplication().addApplicationListener(new ApplicationListener() {
      @Override
      public void writeActionStarted(@NotNull Object action) {
        myUpToDateIndicesForUnsavedOrTransactedDocuments.clear();
      }
    }, this);

    myChangedFilesCollector = new ChangedFilesCollector();
    myConnection = connection;

    VirtualFileManager.getInstance().addAsyncFileListener(myChangedFilesCollector, this);

    initComponent();
  }

  @VisibleForTesting
  void doClearIndices() {
    waitUntilIndicesAreInitialized();
    IndexingStamp.flushCaches();
    for (ID<?, ?> indexId : getState().getIndexIDs()) {
      try {
        clearIndex(indexId);
      }
      catch (StorageException e) {
        LOG.info(e);
      }
    }
  }

  boolean processChangedFiles(@NotNull Project project, @NotNull Processor<? super VirtualFile> processor) {
    // avoid missing files when events are processed concurrently
    return Stream.concat(myChangedFilesCollector.getEventMerger().getChangedFiles(),
                         myChangedFilesCollector.myFilesToUpdate.values().stream())
      .filter(filesToBeIndexedForProjectCondition(project))
      .distinct()
      .mapToInt(f -> processor.process(f) ? 1 : 0)
      .allMatch(success -> success == 1);
  }

  public static boolean isProjectOrWorkspaceFile(@NotNull VirtualFile file, @Nullable FileType fileType) {
    return ProjectCoreUtil.isProjectOrWorkspaceFile(file, fileType);
  }

  static boolean belongsToScope(VirtualFile file, VirtualFile restrictedTo, GlobalSearchScope filter) {
    if (!(file instanceof VirtualFileWithId) || !file.isValid()) {
      return false;
    }

    return (restrictedTo == null || Comparing.equal(file, restrictedTo)) &&
           (filter == null || restrictedTo != null || filter.accept(file));
  }

  @Override
  public void requestReindex(@NotNull final VirtualFile file) {
    GistManager.getInstance().invalidateData();
    // todo: this is the same vfs event handling sequence that is produces after events of FileContentUtilCore.reparseFiles
    // but it is more costly than current code, see IDEA-192192
    //myChangedFilesCollector.invalidateIndicesRecursively(file, false);
    //myChangedFilesCollector.buildIndicesForFileRecursively(file, false);
    myChangedFilesCollector.invalidateIndicesRecursively(file, true, myChangedFilesCollector.getEventMerger());
    if (myInitialized) myChangedFilesCollector.ensureUpToDateAsync();
  }

  private void initComponent() {
    myStateFuture = IndexInfrastructure.submitGenesisTask(new FileIndexDataInitialization());

    if (!IndexInfrastructure.ourDoAsyncIndicesInitialization) {
      waitUntilIndicesAreInitialized();
    }
  }

  private void waitUntilIndicesAreInitialized() {
    try {
      myStateFuture.get();
    }
    catch (Throwable t) {
      LOG.error(t);
    }
  }

  /**
   * @return true if registered index requires full rebuild for some reason, e.g. is just created or corrupted
   */
  private static <K, V> boolean registerIndexer(@NotNull final FileBasedIndexExtension<K, V> extension, IndexConfiguration state) throws IOException {
    final ID<K, V> name = extension.getName();
    final int version = extension.getVersion();

    final File versionFile = IndexInfrastructure.getVersionFile(name);
    boolean versionChanged = false;

    if (IndexingStamp.versionDiffers(name, version)) {
      final boolean versionFileExisted = versionFile.exists();

      if (versionFileExisted) {
        versionChanged = true;
        LOG.info("Version has changed for index " + name + ". The index will be rebuilt.");
      } else {
        LOG.info("Index " + name + " will be built.");
      }
      if (extension.hasSnapshotMapping() && versionChanged) {
        FileUtil.deleteWithRenaming(IndexInfrastructure.getPersistentIndexRootDir(name));
      }
      File rootDir = IndexInfrastructure.getIndexRootDir(name);
      if (versionFileExisted) FileUtil.deleteWithRenaming(rootDir);
      IndexingStamp.rewriteVersion(name, version);
    }

    initIndexStorage(extension, version, state);

    return versionChanged;
  }

  private static <K, V> void initIndexStorage(@NotNull FileBasedIndexExtension<K, V> extension,
                                              int version,
                                              IndexConfiguration state)
    throws IOException {
    VfsAwareMapIndexStorage<K, V> storage = null;
    final ID<K, V> name = extension.getName();
    boolean contentHashesEnumeratorOk = false;

    for (int attempt = 0; attempt < 2; attempt++) {
      try {
        if (extension.hasSnapshotMapping()) {
          ContentHashesSupport.initContentHashesEnumerator();
          contentHashesEnumeratorOk = true;
        }

        storage = new VfsAwareMapIndexStorage<>(
          IndexInfrastructure.getStorageFile(name),
          extension.getKeyDescriptor(),
          extension.getValueExternalizer(),
          extension.getCacheSize(),
          extension.keyIsUniqueForIndexedFile(),
          extension.traceKeyHashToVirtualFileMapping()
        );

        final InputFilter inputFilter = extension.getInputFilter();
        final Set<FileType> addedTypes = new THashSet<>();

        if (inputFilter instanceof FileBasedIndex.FileTypeSpecificInputFilter) {
          ((FileBasedIndex.FileTypeSpecificInputFilter)inputFilter).registerFileTypesUsedForIndexing(type -> {
            if (type != null) addedTypes.add(type);
          });
        }

        state.registerIndex(name,
                            createIndex(extension, new MemoryIndexStorage<>(storage, name)),
                            file -> file instanceof VirtualFileWithId && inputFilter.acceptInput(file) &&
                                    !GlobalIndexFilter.isExcludedFromIndexViaFilters(file, name),
                            version + GlobalIndexFilter.getFiltersVersion(name),
                            addedTypes);
        break;
      }
      catch (Exception e) {
        LOG.info(e);
        boolean instantiatedStorage = storage != null;
        try {
          if (storage != null) storage.close();
          storage = null;
        }
        catch (Exception ignored) {
        }

        FileUtil.deleteWithRenaming(IndexInfrastructure.getIndexRootDir(name));

        if (extension.hasSnapshotMapping() && (!contentHashesEnumeratorOk || instantiatedStorage)) {
          FileUtil.deleteWithRenaming(IndexInfrastructure.getPersistentIndexRootDir(name)); // todo there is possibility of corruption of storage and content hashes
        }
        IndexingStamp.rewriteVersion(name, version);
      }
    }
  }

  private static void saveRegisteredIndicesAndDropUnregisteredOnes(@NotNull Collection<? extends ID<?, ?>> ids) {
    if (ApplicationManager.getApplication().isDisposed() || !IndexInfrastructure.hasIndices()) {
      return;
    }
    final File registeredIndicesFile = new File(PathManager.getIndexRoot(), "registered");
    final Set<String> indicesToDrop = new THashSet<>();
    try (DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(registeredIndicesFile)))) {
      final int size = in.readInt();
      for (int idx = 0; idx < size; idx++) {
        indicesToDrop.add(IOUtil.readString(in));
      }
    }
    catch (IOException ignored) {
    }
    for (ID<?, ?> key : ids) {
      indicesToDrop.remove(key.getName());
    }
    if (!indicesToDrop.isEmpty()) {
      LOG.info("Dropping indices:" + StringUtil.join(indicesToDrop, ","));
      for (String s : indicesToDrop) {
        FileUtil.deleteWithRenaming(IndexInfrastructure.getIndexRootDir(ID.create(s)));
      }
    }

    FileUtil.createIfDoesntExist(registeredIndicesFile);
    try (DataOutputStream os = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(registeredIndicesFile)))) {
      os.writeInt(ids.size());
      for (ID<?, ?> id : ids) {
        IOUtil.writeString(id.getName(), os);
      }
    }
    catch (IOException ignored) {
    }
  }

  @NotNull
  private static <K, V> UpdatableIndex<K, V, FileContent> createIndex(@NotNull final FileBasedIndexExtension<K, V> extension,
                                                                      @NotNull final MemoryIndexStorage<K, V> storage)
    throws StorageException, IOException {
    return extension instanceof CustomImplementationFileBasedIndexExtension
           ? ((CustomImplementationFileBasedIndexExtension<K, V>)extension).createIndexImplementation(extension, storage)
           : new VfsAwareMapReduceIndex<>(extension, storage);
  }

  @Override
  public void dispose() {
    performShutdown();
  }

  private final AtomicBoolean myShutdownPerformed = new AtomicBoolean(false);

  private void performShutdown() {
    if (!myShutdownPerformed.compareAndSet(false, true)) {
      return; // already shut down
    }

    waitUntilAllIndicesAreInitialized();
    try {
      if (myFlushingFuture != null) {
        myFlushingFuture.cancel(false);
        myFlushingFuture = null;
      }
    }
    finally {
      LOG.info("START INDEX SHUTDOWN");
      try {
        PersistentIndicesConfiguration.saveConfiguration();

        for(VirtualFile file:myChangedFilesCollector.getAllFilesToUpdate()) {
          if (!file.isValid()) {
            removeDataFromIndicesForFile(Math.abs(getIdMaskingNonIdBasedFile(file)), file);
          }
        }
        IndexingStamp.flushCaches();

        IndexConfiguration state = getState();
        for (ID<?, ?> indexId : state.getIndexIDs()) {
          try {
            final UpdatableIndex<?, ?, FileContent> index = state.getIndex(indexId);
            assert index != null;
            if (!RebuildStatus.isOk(indexId)) {
              index.clear(); // if the index was scheduled for rebuild, only clean it
            }
            index.dispose();
          } catch (Throwable throwable) {
            LOG.info("Problem disposing " + indexId, throwable);
          }
        }

        ContentHashesSupport.flushContentHashes();
        SharedIndicesData.flushData();
        myConnection.disconnect();
      }
      catch (Throwable e) {
        LOG.error("Problems during index shutdown", e);
      }
      LOG.info("END INDEX SHUTDOWN");
    }
  }

  private void waitUntilAllIndicesAreInitialized() {
    try {
      waitUntilIndicesAreInitialized();
      myAllIndicesInitializedFuture.get();
    } catch (Throwable ignore) {}
  }

  private void removeDataFromIndicesForFile(int fileId, VirtualFile file) {
    VirtualFile originalFile = file instanceof DeletedVirtualFileStub ? ((DeletedVirtualFileStub)file).getOriginalFile() : file;
    final List<ID<?, ?>> states = IndexingStamp.getNontrivialFileIndexedStates(fileId);

    if (!states.isEmpty()) {
      ProgressManager.getInstance().executeNonCancelableSection(() -> removeFileDataFromIndices(states, fileId, originalFile));
    }
  }

  private void removeFileDataFromIndices(@NotNull Collection<? extends ID<?, ?>> affectedIndices, int inputId, VirtualFile file) {
    // document diff can depend on previous value that will be removed
    removeTransientFileDataFromIndices(affectedIndices, inputId, file);

    Throwable unexpectedError = null;
    for (ID<?, ?> indexId : affectedIndices) {
      try {
        updateSingleIndex(indexId, null, inputId, null);
      }
      catch (ProcessCanceledException pce) {
        LOG.error(pce);
      }
      catch (Throwable e) {
        LOG.info(e);
        if (unexpectedError == null) {
          unexpectedError = e;
        }
      }
    }
    IndexingStamp.flushCache(inputId);

    if (unexpectedError != null) {
      LOG.error(unexpectedError);
    }
  }

  private void removeTransientFileDataFromIndices(Collection<? extends ID<?, ?>> indices, int inputId, VirtualFile file) {
    for (ID<?, ?> indexId : indices) {
      final MapReduceIndex index = (MapReduceIndex)myState.getIndex(indexId);
      assert index != null;
      index.removeTransientDataForFile(inputId);
    }

    Document document = myFileDocumentManager.getCachedDocument(file);
    if (document != null) {
      myLastIndexedDocStamps.clearForDocument(document);
      document.putUserData(ourFileContentKey, null);
    }

    if (!myUpToDateIndicesForUnsavedOrTransactedDocuments.isEmpty()) {
      myUpToDateIndicesForUnsavedOrTransactedDocuments.clear();
    }
  }

  private void flushAllIndices(final long modCount) {
    if (HeavyProcessLatch.INSTANCE.isRunning()) {
      return;
    }
    IndexingStamp.flushCaches();
    IndexConfiguration state = getState();
    for (ID<?, ?> indexId : new ArrayList<>(state.getIndexIDs())) {
      if (HeavyProcessLatch.INSTANCE.isRunning() || modCount != myLocalModCount.get()) {
        return; // do not interfere with 'main' jobs
      }
      try {
        final UpdatableIndex<?, ?, FileContent> index = state.getIndex(indexId);
        if (index != null) {
          index.flush();
        }
      }
      catch (Throwable e) {
        requestRebuild(indexId, e);
      }
    }

    ContentHashesSupport.flushContentHashes();
    SharedIndicesData.flushData();
  }

  @Override
  @NotNull
  public <K> Collection<K> getAllKeys(@NotNull final ID<K, ?> indexId, @NotNull Project project) {
    Set<K> allKeys = new THashSet<>();
    processAllKeys(indexId, Processors.cancelableCollectProcessor(allKeys), project);
    return allKeys;
  }

  @Override
  public <K> boolean processAllKeys(@NotNull final ID<K, ?> indexId, @NotNull Processor<? super K> processor, @Nullable Project project) {
    return processAllKeys(indexId, processor, project == null ? new EverythingGlobalScope() : GlobalSearchScope.allScope(project), null);
  }

  @Override
  public <K> boolean processAllKeys(@NotNull ID<K, ?> indexId, @NotNull Processor<? super K> processor, @NotNull GlobalSearchScope scope, @Nullable IdFilter idFilter) {
    try {
      waitUntilIndicesAreInitialized();
      final UpdatableIndex<K, ?, FileContent> index = getIndex(indexId);
      if (index == null) {
        return true;
      }
      ensureUpToDate(indexId, scope.getProject(), scope);
      return index.processAllKeys(processor, scope, idFilter);
    }
    catch (StorageException e) {
      scheduleRebuild(indexId, e);
    }
    catch (RuntimeException e) {
      final Throwable cause = e.getCause();
      if (cause instanceof StorageException || cause instanceof IOException) {
        scheduleRebuild(indexId, cause);
      }
      else {
        throw e;
      }
    }

    return false;
  }

  @NotNull
  @Override
  public <K, V> Map<K, V> getFileData(@NotNull ID<K, V> id, @NotNull VirtualFile virtualFile, @NotNull Project project) {
    int fileId = getFileId(virtualFile);
    Map<K, V> map = processExceptions(id, virtualFile, GlobalSearchScope.fileScope(project, virtualFile), index -> index.getIndexedFileData(fileId));
    return ContainerUtil.notNullize(map);
  }

  private static final ThreadLocal<Integer> myUpToDateCheckState = new ThreadLocal<>();

  public static <T,E extends Throwable> T disableUpToDateCheckIn(@NotNull ThrowableComputable<T, E> runnable) throws E {
    disableUpToDateCheckForCurrentThread();
    try {
      return runnable.compute();
    }
    finally {
      enableUpToDateCheckForCurrentThread();
    }
  }
  private static void disableUpToDateCheckForCurrentThread() {
    final Integer currentValue = myUpToDateCheckState.get();
    myUpToDateCheckState.set(currentValue == null ? 1 : currentValue.intValue() + 1);
  }

  private static void enableUpToDateCheckForCurrentThread() {
    final Integer currentValue = myUpToDateCheckState.get();
    if (currentValue != null) {
      final int newValue = currentValue.intValue() - 1;
      if (newValue != 0) {
        myUpToDateCheckState.set(newValue);
      }
      else {
        myUpToDateCheckState.remove();
      }
    }
  }

  private static boolean isUpToDateCheckEnabled() {
    final Integer value = myUpToDateCheckState.get();
    return value == null || value.intValue() == 0;
  }


  private final ThreadLocal<Boolean> myReentrancyGuard = ThreadLocal.withInitial(() -> Boolean.FALSE);

  /**
   * DO NOT CALL DIRECTLY IN CLIENT CODE
   * The method is internal to indexing engine end is called internally. The method is public due to implementation details
   */
  @Override
  public <K> void ensureUpToDate(@NotNull final ID<K, ?> indexId, @Nullable Project project, @Nullable GlobalSearchScope filter) {
    waitUntilIndicesAreInitialized();
    ensureUpToDate(indexId, project, filter, null);
  }

  protected <K> void ensureUpToDate(@NotNull final ID<K, ?> indexId,
                                    @Nullable Project project,
                                    @Nullable GlobalSearchScope filter,
                                    @Nullable VirtualFile restrictedFile) {
    ProgressManager.checkCanceled();
    myChangedFilesCollector.ensureUpToDate();
    ApplicationManager.getApplication().assertReadAccessAllowed();
    if (!needsFileContentLoading(indexId)) {
      return; //indexed eagerly in foreground while building unindexed file list
    }
    if (filter == GlobalSearchScope.EMPTY_SCOPE) {
      return;
    }
    if (ActionUtil.isDumbMode(project)) {
      handleDumbMode(project);
    }

    NoAccessDuringPsiEvents.checkCallContext();

    if (myReentrancyGuard.get().booleanValue()) {
      //assert false : "ensureUpToDate() is not reentrant!";
      return;
    }
    myReentrancyGuard.set(Boolean.TRUE);

    try {
      if (isUpToDateCheckEnabled()) {
        try {
          if (!RebuildStatus.isOk(indexId)) {
            throw new ServiceNotReadyException();
          }
          forceUpdate(project, filter, restrictedFile);
          indexUnsavedDocuments(indexId, project, filter, restrictedFile);
        }
        catch (RuntimeException e) {
          final Throwable cause = e.getCause();
          if (cause instanceof StorageException || cause instanceof IOException) {
            scheduleRebuild(indexId, e);
          }
          else {
            throw e;
          }
        }
      }
    }
    finally {
      myReentrancyGuard.set(Boolean.FALSE);
    }
  }

  private static void handleDumbMode(@Nullable Project project) {
    ProgressManager.checkCanceled(); // DumbModeAction.CANCEL

    if (project != null) {
      final ProgressIndicator progressIndicator = ProgressManager.getInstance().getProgressIndicator();
      if (progressIndicator instanceof BackgroundableProcessIndicator) {
        final BackgroundableProcessIndicator indicator = (BackgroundableProcessIndicator)progressIndicator;
        if (indicator.getDumbModeAction() == DumbModeAction.WAIT) {
          assert !ApplicationManager.getApplication().isDispatchThread();
          DumbService.getInstance(project).waitForSmartMode();
          return;
        }
      }
    }

    throw IndexNotReadyException.create(project == null ? null : DumbServiceImpl.getInstance(project).getDumbModeStartTrace());
  }

  @Override
  @NotNull
  public <K, V> List<V> getValues(@NotNull final ID<K, V> indexId, @NotNull K dataKey, @NotNull final GlobalSearchScope filter) {
    VirtualFile restrictToFile = null;

    if (filter instanceof Iterable) {
      // optimisation: in case of one-file-scope we can do better.
      // check if the scope knows how to extract some files off itself
      //noinspection unchecked
      Iterator<VirtualFile> virtualFileIterator = ((Iterable<VirtualFile>)filter).iterator();
      if (virtualFileIterator.hasNext()) {
        VirtualFile restrictToFileCandidate = virtualFileIterator.next();
        if (!virtualFileIterator.hasNext()) {
          restrictToFile = restrictToFileCandidate;
        }
      }
    }

    final List<V> values = new SmartList<>();
    ValueProcessor<V> processor = (file, value) -> {
      values.add(value);
      return true;
    };
    if (restrictToFile != null) {
      processValuesInOneFile(indexId, dataKey, restrictToFile, processor, filter);
    }
    else {
      processValuesInScope(indexId, dataKey, true, filter, null, processor);
    }
    return values;
  }

  @Override
  @NotNull
  public <K, V> Collection<VirtualFile> getContainingFiles(@NotNull final ID<K, V> indexId,
                                                           @NotNull K dataKey,
                                                           @NotNull final GlobalSearchScope filter) {
    final Set<VirtualFile> files = new THashSet<>();
    processValuesInScope(indexId, dataKey, false, filter, null, (file, value) -> {
      files.add(file);
      return true;
    });
    return files;
  }


  @Override
  public <K, V> boolean processValues(@NotNull final ID<K, V> indexId, @NotNull final K dataKey, @Nullable final VirtualFile inFile,
                                      @NotNull ValueProcessor<? super V> processor, @NotNull final GlobalSearchScope filter) {
    return processValues(indexId, dataKey, inFile, processor, filter, null);
  }

  @Override
  public <K, V> boolean processValues(@NotNull ID<K, V> indexId,
                                      @NotNull K dataKey,
                                      @Nullable VirtualFile inFile,
                                      @NotNull ValueProcessor<? super V> processor,
                                      @NotNull GlobalSearchScope filter,
                                      @Nullable IdFilter idFilter) {
    return inFile != null
           ? processValuesInOneFile(indexId, dataKey, inFile, processor, filter)
           : processValuesInScope(indexId, dataKey, false, filter, idFilter, processor);
  }

  public <K, V> long getIndexModificationStamp(ID<K, V> indexId, @NotNull Project project) {
    UpdatableIndex<K, V, FileContent> index = getState().getIndex(indexId);
    if (index instanceof MapReduceIndex) {
      ensureUpToDate(indexId, project, GlobalSearchScope.allScope(project));
      return ((MapReduceIndex)index).getModificationStamp();
    }
    return -1;
  }

  @FunctionalInterface
  public interface IdValueProcessor<V> {
    /**
     * @param fileId the id of the file that the value came from
     * @param value a value to process
     * @return false if no further processing is needed, true otherwise
     */
    boolean process(int fileId, V value);
  }

  /**
   * Process values for a given index key together with their containing file ids. Note that project is supplied
   * only to ensure that all the indices in that project are up to date; there's no guarantee that the processed file ids belong
   * to this project.
   */
  public <K, V> boolean processAllValues(@NotNull ID<K, V> indexId,
                                         @NotNull K key,
                                         @NotNull Project project,
                                         @NotNull IdValueProcessor<? super V> processor) {
    return processValueIterator(indexId, key, null, GlobalSearchScope.allScope(project), valueIt -> {
      while (valueIt.hasNext()) {
        V value = valueIt.next();
        for (ValueContainer.IntIterator inputIdsIterator = valueIt.getInputIdsIterator(); inputIdsIterator.hasNext(); ) {
          if (!processor.process(inputIdsIterator.next(), value)) {
            return false;
          }
          ProgressManager.checkCanceled();
        }
      }
      return true;
    });
  }

  @Nullable
  private <K, V, R> R processExceptions(@NotNull final ID<K, V> indexId,
                                        @Nullable final VirtualFile restrictToFile,
                                        @NotNull final GlobalSearchScope filter,
                                        @NotNull ThrowableConvertor<? super UpdatableIndex<K, V, FileContent>, ? extends R, ? extends StorageException> computable) {
    try {
      waitUntilIndicesAreInitialized();
      final UpdatableIndex<K, V, FileContent> index = getIndex(indexId);
      if (index == null) {
        return null;
      }
      final Project project = filter.getProject();
      //assert project != null : "GlobalSearchScope#getProject() should be not-null for all index queries";
      ensureUpToDate(indexId, project, filter, restrictToFile);

      return myAccessValidator.validate(indexId, ()->ConcurrencyUtil.withLock(index.getReadLock(), ()->computable.convert(index)));
    }
    catch (StorageException e) {
      scheduleRebuild(indexId, e);
    }
    catch (RuntimeException e) {
      final Throwable cause = getCauseToRebuildIndex(e);
      if (cause != null) {
        scheduleRebuild(indexId, cause);
      }
      else {
        throw e;
      }
    }
    return null;
  }

  private <K, V> boolean processValuesInOneFile(@NotNull ID<K, V> indexId,
                                                @NotNull K dataKey,
                                                @NotNull VirtualFile restrictToFile,
                                                @NotNull ValueProcessor<? super V> processor, @NotNull GlobalSearchScope scope) {
    if (!(restrictToFile instanceof VirtualFileWithId)) return true;

    int restrictedFileId = getFileId(restrictToFile);
    return processValueIterator(indexId, dataKey, restrictToFile, scope, valueIt -> {
      while (valueIt.hasNext()) {
        V value = valueIt.next();
        if (valueIt.getValueAssociationPredicate().contains(restrictedFileId) && !processor.process(restrictToFile, value)) {
          return false;
        }
        ProgressManager.checkCanceled();
      }
      return true;
    });
  }

  private <K, V> boolean processValuesInScope(@NotNull ID<K, V> indexId,
                                              @NotNull K dataKey,
                                              boolean ensureValueProcessedOnce,
                                              @NotNull GlobalSearchScope scope,
                                              @Nullable IdFilter idFilter,
                                              @NotNull ValueProcessor<? super V> processor) {
    PersistentFS fs = (PersistentFS)ManagingFS.getInstance();
    IdFilter filter = idFilter != null ? idFilter : projectIndexableFiles(scope.getProject());

    return processValueIterator(indexId, dataKey, null, scope, valueIt -> {
      while (valueIt.hasNext()) {
        final V value = valueIt.next();
        for (final ValueContainer.IntIterator inputIdsIterator = valueIt.getInputIdsIterator(); inputIdsIterator.hasNext(); ) {
          final int id = inputIdsIterator.next();
          if (filter != null && !filter.containsFileId(id)) continue;
          VirtualFile file = IndexInfrastructure.findFileByIdIfCached(fs, id);
          if (file != null && scope.accept(file)) {
            if (!processor.process(file, value)) {
              return false;
            }
            if (ensureValueProcessedOnce) {
              ProgressManager.checkCanceled();
              break; // continue with the next value
            }
          }

          ProgressManager.checkCanceled();
        }
      }
      return true;
    });
  }

  private <K, V> boolean processValueIterator(@NotNull ID<K, V> indexId,
                                              @NotNull K dataKey,
                                              @Nullable VirtualFile restrictToFile,
                                              @NotNull GlobalSearchScope scope,
                                              @NotNull Processor<? super InvertedIndexValueIterator<V>> valueProcessor) {
    final Boolean result = processExceptions(indexId, restrictToFile, scope,
                                             index -> valueProcessor.process((InvertedIndexValueIterator<V>)index.getData(dataKey).getValueIterator()));
    return result == null || result.booleanValue();
  }

  @Override
  public <K, V> boolean processFilesContainingAllKeys(@NotNull final ID<K, V> indexId,
                                                      @NotNull final Collection<? extends K> dataKeys,
                                                      @NotNull final GlobalSearchScope filter,
                                                      @Nullable Condition<? super V> valueChecker,
                                                      @NotNull final Processor<? super VirtualFile> processor) {
    ProjectIndexableFilesFilter filesSet = projectIndexableFiles(filter.getProject());
    final TIntHashSet set = collectFileIdsContainingAllKeys(indexId, dataKeys, filter, valueChecker, filesSet);
    return set != null && processVirtualFiles(set, filter, processor);
  }

  private static final Key<SoftReference<ProjectIndexableFilesFilter>> ourProjectFilesSetKey = Key.create("projectFiles");

  void filesUpdateEnumerationFinished() {
  }

  @TestOnly
  public void cleanupForNextTest() {
    myChangedFilesCollector.ensureUpToDate();

    myTransactionMap = SmartFMap.emptyMap();
    IndexConfiguration state = getState();
    for (ID<?, ?> indexId : state.getIndexIDs()) {
      final MapReduceIndex index = (MapReduceIndex)state.getIndex(indexId);
      assert index != null;
      final MemoryIndexStorage memStorage = (MemoryIndexStorage)index.getStorage();
      ConcurrencyUtil.withLock(index.getReadLock(), () -> memStorage.clearCaches());
    }
  }

  @TestOnly
  public IndexedFilesListener getChangedFilesCollector() {
    return myChangedFilesCollector;
  }

  public static final class ProjectIndexableFilesFilter extends IdFilter {
    private static final int SHIFT = 6;
    private static final int MASK = (1 << SHIFT) - 1;
    private final long[] myBitMask;
    private final int myModificationCount;
    private final int myMinId;
    private final int myMaxId;

    private ProjectIndexableFilesFilter(@NotNull TIntArrayList set, int modificationCount) {
      myModificationCount = modificationCount;
      final int[] minMax = new int[2];
      if (!set.isEmpty()) {
        minMax[0] = minMax[1] = set.get(0);
      }
      set.forEach(value -> {
        minMax[0] = Math.min(minMax[0], value);
        minMax[1] = Math.max(minMax[1], value);
        return true;
      });
      myMaxId = minMax[1];
      myMinId = minMax[0];
      myBitMask = new long[((myMaxId - myMinId) >> SHIFT) + 1];
      set.forEach(value -> {
        value -= myMinId;
        myBitMask[value >> SHIFT] |= (1L << (value & MASK));
        return true;
      });
    }

    @Override
    public boolean containsFileId(int id) {
      if (id < myMinId) return false;
      if (id > myMaxId) return false;
      id -= myMinId;
      return (myBitMask[id >> SHIFT] & (1L << (id & MASK))) != 0;
    }
  }

  void filesUpdateStarted(Project project) {
    myChangedFilesCollector.ensureUpToDate();
    myProjectsBeingUpdated.add(project);
    myFilesModCount.incrementAndGet();
  }

  void filesUpdateFinished(@NotNull Project project) {
    myProjectsBeingUpdated.remove(project);
    myFilesModCount.incrementAndGet();
  }

  private final Lock myCalcIndexableFilesLock = new ReentrantLock();

  @Nullable
  public ProjectIndexableFilesFilter projectIndexableFiles(@Nullable Project project) {
    if (project == null || myUpdatingFiles.get() > 0) return null;
    if (myProjectsBeingUpdated.contains(project)) return null;

    SoftReference<ProjectIndexableFilesFilter> reference = project.getUserData(ourProjectFilesSetKey);
    ProjectIndexableFilesFilter data = com.intellij.reference.SoftReference.dereference(reference);
    int currentFileModCount = myFilesModCount.get();
    if (data != null && data.myModificationCount == currentFileModCount) return data;

    if (myCalcIndexableFilesLock.tryLock()) { // make best effort for calculating filter
      try {
        reference = project.getUserData(ourProjectFilesSetKey);
        data = com.intellij.reference.SoftReference.dereference(reference);
        if (data != null && data.myModificationCount == currentFileModCount) {
          return data;
        }

        long start = System.currentTimeMillis();

        final TIntArrayList filesSet = new TIntArrayList();
        iterateIndexableFiles(fileOrDir -> {
          ProgressManager.checkCanceled();
          if (fileOrDir instanceof VirtualFileWithId) {
            filesSet.add(((VirtualFileWithId)fileOrDir).getId());
          }
          return true;
        }, project, SilentProgressIndicator.create());
        ProjectIndexableFilesFilter filter = new ProjectIndexableFilesFilter(filesSet, currentFileModCount);
        project.putUserData(ourProjectFilesSetKey, new SoftReference<>(filter));

        long finish = System.currentTimeMillis();
        LOG.debug(filesSet.size() + " files iterated in " + (finish - start) + " ms");

        return filter;
      }
      finally {
        myCalcIndexableFilesLock.unlock();
      }
    }
    return null; // ok, no filtering
  }

  @Nullable
  private <K, V> TIntHashSet collectFileIdsContainingAllKeys(@NotNull final ID<K, V> indexId,
                                                             @NotNull final Collection<? extends K> dataKeys,
                                                             @NotNull final GlobalSearchScope filter,
                                                             @Nullable final Condition<? super V> valueChecker,
                                                             @Nullable final ProjectIndexableFilesFilter projectFilesFilter) {
    ThrowableConvertor<UpdatableIndex<K, V, FileContent>, TIntHashSet, StorageException> convertor =
      index -> InvertedIndexUtil.collectInputIdsContainingAllKeys(index, dataKeys, __ -> {
                                                                    ProgressManager.checkCanceled();
                                                                    return true;
                                                                  }, valueChecker,
                                                projectFilesFilter == null ? null : projectFilesFilter::containsFileId);

    return processExceptions(indexId, null, filter, convertor);
  }

  private static boolean processVirtualFiles(@NotNull TIntHashSet ids,
                                             @NotNull final GlobalSearchScope filter,
                                             @NotNull final Processor<? super VirtualFile> processor) {
    final PersistentFS fs = (PersistentFS)ManagingFS.getInstance();
    return ids.forEach(id -> {
      ProgressManager.checkCanceled();
      VirtualFile file = IndexInfrastructure.findFileByIdIfCached(fs, id);
      if (file != null && filter.accept(file)) {
        return processor.process(file);
      }
      return true;
    });
  }

  @Nullable
  public static Throwable getCauseToRebuildIndex(@NotNull RuntimeException e) {
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      // avoid rebuilding index in tests since we do it synchronously in requestRebuild and we can have readAction at hand
      return null;
    }
    if (e instanceof ProcessCanceledException) return null;
    if (e instanceof IndexOutOfBoundsException) return e; // something wrong with direct byte buffer
    Throwable cause = e.getCause();
    if (cause instanceof StorageException || cause instanceof IOException ||
        cause instanceof IllegalArgumentException) return cause;
    return null;
  }

  @Override
  public <K, V> boolean getFilesWithKey(@NotNull final ID<K, V> indexId,
                                        @NotNull final Set<? extends K> dataKeys,
                                        @NotNull Processor<? super VirtualFile> processor,
                                        @NotNull GlobalSearchScope filter) {
    return processFilesContainingAllKeys(indexId, dataKeys, filter, null, processor);
  }

  @Override
  public <K> void scheduleRebuild(@NotNull final ID<K, ?> indexId, @NotNull final Throwable e) {
    requestRebuild(indexId, e);
  }

  private static void scheduleIndexRebuild(String reason) {
    LOG.info("scheduleIndexRebuild, reason: " + reason);
    for (Project project : ProjectManager.getInstance().getOpenProjects()) {
      DumbService.getInstance(project).queueTask(new UnindexedFilesUpdater(project));
    }
  }

  void clearIndicesIfNecessary() {
    waitUntilIndicesAreInitialized();
    for (ID<?, ?> indexId : getState().getIndexIDs()) {
      try {
        RebuildStatus.clearIndexIfNecessary(indexId, getIndex(indexId)::clear);
      }
      catch (StorageException e) {
        requestRebuild(indexId);
        LOG.error(e);
      }
    }
  }

  private void clearIndex(@NotNull final ID<?, ?> indexId) throws StorageException {
    advanceIndexVersion(indexId);

    final UpdatableIndex<?, ?, FileContent> index = myState.getIndex(indexId);
    assert index != null : "Index with key " + indexId + " not found or not registered properly";
    index.clear();
  }

  private void advanceIndexVersion(ID<?, ?> indexId) {
    try {
      IndexingStamp.rewriteVersion(indexId, myState.getIndexVersion(indexId));
    }
    catch (IOException e) {
      LOG.error(e);
    }
  }

  @NotNull
  private Set<Document> getUnsavedDocuments() {
    Document[] documents = myFileDocumentManager.getUnsavedDocuments();
    if (documents.length == 0) return Collections.emptySet();
    if (documents.length == 1) return Collections.singleton(documents[0]);
    return new THashSet<>(Arrays.asList(documents));
  }

  @NotNull
  private Set<Document> getTransactedDocuments() {
    return myTransactionMap.keySet();
  }

  private void indexUnsavedDocuments(@NotNull final ID<?, ?> indexId,
                                     @Nullable Project project,
                                     final GlobalSearchScope filter,
                                     final VirtualFile restrictedFile) {
    if (myUpToDateIndicesForUnsavedOrTransactedDocuments.contains(indexId)) {
      return; // no need to index unsaved docs        // todo: check scope ?
    }

    Collection<Document> documents = getUnsavedDocuments();
    boolean psiBasedIndex = myPsiDependentIndices.contains(indexId);
    if(psiBasedIndex) {
      Set<Document> transactedDocuments = getTransactedDocuments();
      if (documents.isEmpty()) {
        documents = transactedDocuments;
      }
      else if (!transactedDocuments.isEmpty()) {
        documents = new THashSet<>(documents);
        documents.addAll(transactedDocuments);
      }
      Document[] uncommittedDocuments = project != null ? PsiDocumentManager.getInstance(project).getUncommittedDocuments() : Document.EMPTY_ARRAY;
      if (uncommittedDocuments.length > 0) {
        List<Document> uncommittedDocumentsCollection = Arrays.asList(uncommittedDocuments);
        if (documents.isEmpty()) documents = uncommittedDocumentsCollection;
        else {
          if (!(documents instanceof THashSet)) documents = new THashSet<>(documents);

          documents.addAll(uncommittedDocumentsCollection);
        }
      }
    }

    if (!documents.isEmpty()) {
      Collection<Document> documentsToProcessForProject = ContainerUtil.filter(documents,
                                                                               document -> belongsToScope(myFileDocumentManager.getFile(document), restrictedFile, filter));

      if (!documentsToProcessForProject.isEmpty()) {
        DocumentUpdateTask task = myUnsavedDataUpdateTasks.get(indexId);
        assert task != null : "Task for unsaved data indexing was not initialized for index " + indexId;

        if(runUpdate(true, () -> task.processAll(documentsToProcessForProject, project)) &&
           documentsToProcessForProject.size() == documents.size() &&
           !hasActiveTransactions()
          ) {
          ProgressManager.checkCanceled();
          myUpToDateIndicesForUnsavedOrTransactedDocuments.add(indexId);
        }
      }
    }
  }

  private boolean hasActiveTransactions() {
    return !myTransactionMap.isEmpty();
  }

  private interface DocumentContent {
    @NotNull
    CharSequence getText();

    long getModificationStamp();
  }

  private static class AuthenticContent implements DocumentContent {
    private final Document myDocument;

    private AuthenticContent(final Document document) {
      myDocument = document;
    }

    @NotNull
    @Override
    public CharSequence getText() {
      return myDocument.getImmutableCharSequence();
    }

    @Override
    public long getModificationStamp() {
      return myDocument.getModificationStamp();
    }
  }

  private static class PsiContent implements DocumentContent {
    private final Document myDocument;
    private final PsiFile myFile;

    private PsiContent(final Document document, final PsiFile file) {
      myDocument = document;
      myFile = file;
    }

    @NotNull
    @Override
    public CharSequence getText() {
      if (myFile.getViewProvider().getModificationStamp() != myDocument.getModificationStamp()) {
        final ASTNode node = myFile.getNode();
        assert node != null;
        return node.getChars();
      }
      return myDocument.getImmutableCharSequence();
    }

    @Override
    public long getModificationStamp() {
      return myFile.getViewProvider().getModificationStamp();
    }
  }

  private static final Key<WeakReference<FileContentImpl>> ourFileContentKey = Key.create("unsaved.document.index.content");

  // returns false if doc was not indexed because it is already up to date
  // return true if document was indexed
  // caller is responsible to ensure no concurrent same document processing
  private boolean indexUnsavedDocument(@NotNull final Document document, @NotNull final ID<?, ?> requestedIndexId, final Project project,
                                       @NotNull final VirtualFile vFile) {
    final PsiFile dominantContentFile = project == null ? null : findLatestKnownPsiForUncomittedDocument(document, project);

    final DocumentContent content;
    if (dominantContentFile != null && dominantContentFile.getViewProvider().getModificationStamp() != document.getModificationStamp()) {
      content = new PsiContent(document, dominantContentFile);
    }
    else {
      content = new AuthenticContent(document);
    }

    boolean psiBasedIndex = myPsiDependentIndices.contains(requestedIndexId);

    final long currentDocStamp = psiBasedIndex ? PsiDocumentManager.getInstance(project).getLastCommittedStamp(document) : content.getModificationStamp();

    final long previousDocStamp = myLastIndexedDocStamps.get(document, requestedIndexId);
    if (previousDocStamp == currentDocStamp) return false;

    final CharSequence contentText = content.getText();
    myFileTypeManager.freezeFileTypeTemporarilyIn(vFile, () -> {
      if (getAffectedIndexCandidates(vFile).contains(requestedIndexId) &&
          getInputFilter(requestedIndexId).acceptInput(vFile)) {
        final int inputId = Math.abs(getFileId(vFile));

        if (!isTooLarge(vFile, contentText.length())) {
          // Reasonably attempt to use same file content when calculating indices as we can evaluate them several at once and store in file content
          WeakReference<FileContentImpl> previousContentRef = document.getUserData(ourFileContentKey);
          FileContentImpl previousContent = com.intellij.reference.SoftReference.dereference(previousContentRef);
          final FileContentImpl newFc;
          if (previousContent != null && previousContent.getStamp() == currentDocStamp) {
            newFc = previousContent;
          }
          else {
            newFc = new FileContentImpl(vFile, contentText, currentDocStamp);
            if (IdIndex.ourSnapshotMappingsEnabled) {
              newFc.putUserData(UpdatableSnapshotInputMappingIndex.FORCE_IGNORE_MAPPING_INDEX_UPDATE, Boolean.TRUE);
            }
            document.putUserData(ourFileContentKey, new WeakReference<>(newFc));
          }

          initFileContent(newFc, project, dominantContentFile);
          newFc.ensureThreadSafeLighterAST();

          if (content instanceof AuthenticContent) {
            newFc.putUserData(PlatformIdTableBuilding.EDITOR_HIGHLIGHTER,
                              EditorHighlighterCache.getEditorHighlighterForCachesBuilding(document));
          }

          try {
            getIndex(requestedIndexId).update(inputId, newFc).compute();
          }
          finally {
            cleanFileContent(newFc, dominantContentFile);
          }
        } else { // effectively wipe the data from the indices
          getIndex(requestedIndexId).update(inputId, null).compute();
        }
      }

      long previousState = myLastIndexedDocStamps.set(document, requestedIndexId, currentDocStamp);
      assert previousState == previousDocStamp;
    });

    return true;
  }

  private final StorageGuard myStorageLock = new StorageGuard();
  private volatile boolean myPreviousDataBufferingState;
  private final Object myBufferingStateUpdateLock = new Object();

  @ApiStatus.Experimental
  public void runCleanupAction(@NotNull Runnable cleanupAction) {
    Computable<Boolean> updateComputable = () -> {
      cleanupAction.run();
      return true;
    };
    runUpdate(false, updateComputable);
    runUpdate(true, updateComputable);
  }

  private boolean runUpdate(boolean transientInMemoryIndices, Computable<Boolean> update) {
    StorageGuard.StorageModeExitHandler storageModeExitHandler = myStorageLock.enter(transientInMemoryIndices);

    if (myPreviousDataBufferingState != transientInMemoryIndices) {
      synchronized (myBufferingStateUpdateLock) {
        if (myPreviousDataBufferingState != transientInMemoryIndices) {
          IndexConfiguration state = getState();
          for (ID<?, ?> indexId : state.getIndexIDs()) {
            final MapReduceIndex index = (MapReduceIndex)state.getIndex(indexId);
            assert index != null;
            ((MemoryIndexStorage)index.getStorage()).setBufferingEnabled(transientInMemoryIndices);
          }
          myPreviousDataBufferingState = transientInMemoryIndices;
        }
      }
    }

    try {
      return update.compute();
    } finally {
      storageModeExitHandler.leave();
    }
  }

  private void cleanupMemoryStorage(boolean skipPsiBasedIndices) {
    myLastIndexedDocStamps.clear();
    IndexConfiguration state = myState;
    if (state == null) {
      // avoid waiting for end of indices initialization (IDEA-173382)
      // in memory content will appear on indexing (in read action) and here is event dispatch (write context)
      return;
    }
    for (ID<?, ?> indexId : state.getIndexIDs()) {
      if (skipPsiBasedIndices && myPsiDependentIndices.contains(indexId)) continue;
      final MapReduceIndex index = (MapReduceIndex)state.getIndex(indexId);
      assert index != null;
      final MemoryIndexStorage memStorage = (MemoryIndexStorage)index.getStorage();
      ConcurrencyUtil.withLock(index.getWriteLock(), () -> memStorage.clearMemoryMap());
      memStorage.fireMemoryStorageCleared();
    }
  }

  @Override
  public void requestRebuild(@NotNull final ID<?, ?> indexId, final Throwable throwable) {
    cleanupProcessedFlag();
    if (!myExtensionsRelatedDataWasLoaded) reportUnexpectedAsyncInitState();

    if (RebuildStatus.requestRebuild(indexId)) {
      String message = "Rebuild requested for index " + indexId;
      Application app = ApplicationManager.getApplication();
      if (app.isUnitTestMode() && app.isReadAccessAllowed() && !app.isDispatchThread()) {
        // shouldn't happen in tests in general; so fail early with the exception that caused index to be rebuilt.
        // otherwise reindexing will fail anyway later, but with a much more cryptic assertion
        LOG.error(message, throwable);
      } else {
        LOG.info(message, throwable);
      }

      cleanupProcessedFlag();

      if (!myInitialized) return;
      advanceIndexVersion(indexId);

      Runnable rebuildRunnable = () -> scheduleIndexRebuild("checkRebuild");

      if (myIsUnitTestMode) {
        rebuildRunnable.run();
      }
      else {
        // we do invoke later since we can have read lock acquired
        TransactionGuard.getInstance().submitTransactionLater(app, rebuildRunnable);
      }
    }
  }

  private static void reportUnexpectedAsyncInitState() {
    LOG.error("Unexpected async indices initialization problem");
  }

  public <K, V> UpdatableIndex<K, V, FileContent> getIndex(ID<K, V> indexId) {
    return getState().getIndex(indexId);
  }

  private InputFilter getInputFilter(@NotNull ID<?, ?> indexId) {
    if (!myInitialized) {
      // 1. early vfs event that needs invalidation
      // 2. pushers that do synchronous indexing for contentless indices
      waitUntilIndicesAreInitialized();
    }

    return getState().getInputFilter(indexId);
  }

  @NotNull
  Collection<VirtualFile> getFilesToUpdate(final Project project) {
    return ContainerUtil.filter(myChangedFilesCollector.getAllFilesToUpdate(), filesToBeIndexedForProjectCondition(project)::test);
  }

  @NotNull
  private Predicate<VirtualFile> filesToBeIndexedForProjectCondition(Project project) {
    return virtualFile -> {
        if (!virtualFile.isValid()) {
          return true;
        }

        for (IndexableFileSet set : myIndexableSets) {
          final Project proj = myIndexableSetToProjectMap.get(set);
          if (proj != null && !proj.equals(project)) {
            continue; // skip this set as associated with a different project
          }
          if (ReadAction.compute(() -> set.isInSet(virtualFile))) {
            return true;
          }
        }
        return false;
      };
  }

  public boolean isFileUpToDate(VirtualFile file) {
    return !myChangedFilesCollector.isScheduledForUpdate(file);
  }

  // caller is responsible to ensure no concurrent same document processing
  void processRefreshedFile(@Nullable Project project, @NotNull final com.intellij.ide.caches.FileContent fileContent) {
    // ProcessCanceledException will cause re-adding the file to processing list
    final VirtualFile file = fileContent.getVirtualFile();
    if (myChangedFilesCollector.isScheduledForUpdate(file)) {
      indexFileContent(project, fileContent);
    }
  }

  public void indexFileContent(@Nullable Project project, @NotNull com.intellij.ide.caches.FileContent content) {
    VirtualFile file = content.getVirtualFile();
    final int fileId = Math.abs(getIdMaskingNonIdBasedFile(file));

    try {
      // if file was scheduled for update due to vfs events then it is present in myFilesToUpdate
      // in this case we consider that current indexing (out of roots backed CacheUpdater) will cover its content
      if (file.isValid() && content.getTimeStamp() != file.getTimeStamp()) {
        content = new com.intellij.ide.caches.FileContent(file);
      }
      if (!file.isValid() || isTooLarge(file)) {
        removeDataFromIndicesForFile(fileId, file);
        if (file instanceof DeletedVirtualFileStub && ((DeletedVirtualFileStub)file).isResurrected()) {
          doIndexFileContent(project, new com.intellij.ide.caches.FileContent(((DeletedVirtualFileStub)file).getOriginalFile()));
        }
      }
      else {
        doIndexFileContent(project, content);
      }
    }
    finally {
      IndexingStamp.flushCache(fileId);
    }

    myChangedFilesCollector.removeFileIdFromFilesScheduledForUpdate(fileId);
    if (file instanceof VirtualFileSystemEntry) ((VirtualFileSystemEntry)file).setFileIndexed(true);
  }

  private void doIndexFileContent(@Nullable Project project, @NotNull final com.intellij.ide.caches.FileContent content) {
    final VirtualFile file = content.getVirtualFile();
    myFileTypeManager.freezeFileTypeTemporarilyIn(file, () -> {
      final FileType fileType = file.getFileType();
      final Project finalProject = project == null ? ProjectUtil.guessProjectForFile(file) : project;
      PsiFile psiFile = null;
      FileContentImpl fc = null;
      int inputId = Math.abs(getFileId(file));
      Set<ID<?, ?>> currentIndexedStates = new THashSet<>(IndexingStamp.getNontrivialFileIndexedStates(inputId));

      final List<ID<?, ?>> affectedIndexCandidates = getAffectedIndexCandidates(file);
      //noinspection ForLoopReplaceableByForEach
      for (int i = 0, size = affectedIndexCandidates.size(); i < size; ++i) {
        final ID<?, ?> indexId = affectedIndexCandidates.get(i);
        if (shouldIndexFile(file, indexId)) {
          if (fc == null) {
            byte[] currentBytes;
            try {
              currentBytes = content.getBytes();
            }
            catch (IOException e) {
              currentBytes = ArrayUtilRt.EMPTY_BYTE_ARRAY;
            }
            fc = new FileContentImpl(file, currentBytes);

            if (IdIndex.ourSnapshotMappingsEnabled) {
              FileType substituteFileType = SubstitutedFileType.substituteFileType(file, fileType, finalProject);
              byte[] hash = fileType.isBinary() ?
                            ContentHashesSupport.calcContentHash(currentBytes, substituteFileType) :
                            ContentHashesSupport.calcContentHashWithFileType(
                              currentBytes,
                              fc.getCharset(),
                              substituteFileType
                            );
              fc.setHash(hash);
            }

            psiFile = content.getUserData(IndexingDataKeys.PSI_FILE);
            initFileContent(fc, finalProject, psiFile);
          }

          try {
            ProgressManager.checkCanceled();
            updateSingleIndex(indexId, file, inputId, fc);
            currentIndexedStates.remove(indexId);
          }
          catch (ProcessCanceledException e) {
            cleanFileContent(fc, psiFile);
            throw e;
          }
        }
      }

      if (psiFile != null) {
        psiFile.putUserData(PsiFileImpl.BUILDING_STUB, null);
      }

      for(ID<?, ?> indexId:currentIndexedStates) {
        if(!getIndex(indexId).isIndexedStateForFile(inputId, file)) {
          ProgressManager.checkCanceled();
          updateSingleIndex(indexId, file, inputId, null);
        }
      }
    });
  }

  public boolean isIndexingCandidate(@NotNull VirtualFile file, @NotNull ID<?, ?> indexId) {
    return !isTooLarge(file) && getAffectedIndexCandidates(file).contains(indexId);
  }

  @NotNull
  private List<ID<?, ?>> getAffectedIndexCandidates(@NotNull VirtualFile file) {
    if (file.isDirectory()) {
      return isProjectOrWorkspaceFile(file, null) ? Collections.emptyList() : myIndicesForDirectories;
    }
    FileType fileType = file.getFileType();
    if(isProjectOrWorkspaceFile(file, fileType)) return Collections.emptyList();

    return getState().getFileTypesForIndex(fileType);
  }

  private static void cleanFileContent(@NotNull FileContentImpl fc, PsiFile psiFile) {
    if (psiFile != null) psiFile.putUserData(PsiFileImpl.BUILDING_STUB, null);
    fc.putUserData(IndexingDataKeys.PSI_FILE, null);
  }

  private static void initFileContent(@NotNull FileContentImpl fc, Project project, PsiFile psiFile) {
    if (psiFile != null) {
      psiFile.putUserData(PsiFileImpl.BUILDING_STUB, true);
      fc.putUserData(IndexingDataKeys.PSI_FILE, psiFile);
    }

    fc.putUserData(IndexingDataKeys.PROJECT, project);
  }

  private void updateSingleIndex(@NotNull ID<?, ?> indexId, VirtualFile file, final int inputId, @Nullable FileContent currentFC) {
    if (!myExtensionsRelatedDataWasLoaded) reportUnexpectedAsyncInitState();
    if (!RebuildStatus.isOk(indexId) && !myIsUnitTestMode) {
      return; // the index is scheduled for rebuild, no need to update
    }
    myLocalModCount.incrementAndGet();

    final UpdatableIndex<?, ?, FileContent> index = getIndex(indexId);
    assert index != null;

    boolean hasContent = currentFC != null;

    if (ourIndexedFile.get() != null) throw new AssertionError("Reentrant indexing");
    ourIndexedFile.set(file);
    boolean updateCalculated = false;
    try {
      // important: no hard referencing currentFC to avoid OOME, the methods introduced for this purpose!
      // important: update is called out of try since possible indexer extension is HANDLED as single file fail / restart indexing policy
      final Computable<Boolean> update = index.update(inputId, currentFC);
      updateCalculated = true;

      scheduleUpdate(indexId, update, file, inputId, hasContent);
    }
    catch (RuntimeException exception) {
      Throwable causeToRebuildIndex = getCauseToRebuildIndex(exception);
      if (causeToRebuildIndex != null && (updateCalculated || causeToRebuildIndex instanceof IOException)) {
        requestRebuild(indexId, exception);
        return;
      }
      throw exception;
    }
    finally {
      ourIndexedFile.remove();
    }
  }

  @Override
  public VirtualFile getFileBeingCurrentlyIndexed() {
    return ourIndexedFile.get();
  }

  private class VirtualFileUpdateTask extends UpdateTask<VirtualFile> {
    @Override
    void doProcess(VirtualFile item, Project project) {
      processRefreshedFile(project, new com.intellij.ide.caches.FileContent(item));
    }
  }

  private final VirtualFileUpdateTask myForceUpdateTask = new VirtualFileUpdateTask();
  private final AtomicInteger myForceUpdateRequests = new AtomicInteger();

  private void forceUpdate(@Nullable Project project, @Nullable final GlobalSearchScope filter, @Nullable final VirtualFile restrictedTo) {
    Collection<VirtualFile> allFilesToUpdate = myChangedFilesCollector.getAllFilesToUpdate();

    if (!allFilesToUpdate.isEmpty()) {
      boolean includeFilesFromOtherProjects = restrictedTo == null && (myForceUpdateRequests.incrementAndGet() & 0x3F) == 0;
      List<VirtualFile> virtualFilesToBeUpdatedForProject = ContainerUtil.filter(
        allFilesToUpdate,
        new ProjectFilesCondition(projectIndexableFiles(project), filter, restrictedTo,
                                  includeFilesFromOtherProjects)
      );

      if (!virtualFilesToBeUpdatedForProject.isEmpty()) {
        myForceUpdateTask.processAll(virtualFilesToBeUpdatedForProject, project);
      }
    }
  }

  private final Lock myReadLock;
  private final Lock myWriteLock;
  {
    ReadWriteLock lock = new ReentrantReadWriteLock();
    myReadLock = lock.readLock();
    myWriteLock = lock.writeLock();
  }
  private void scheduleUpdate(@NotNull final ID<?, ?> indexId, @NotNull Computable<Boolean> update, VirtualFile file, final int inputId, final boolean hasContent) {
    if (runUpdate(false, update)) {
      ConcurrencyUtil.withLock(myReadLock, ()->{
        UpdatableIndex<?, ?, FileContent> index = getIndex(indexId);
        if (hasContent) {
          index.setIndexedStateForFile(inputId, file);
        }
        else {
          index.resetIndexedStateForFile(inputId);
        }
      });
    }
  }

  private boolean needsFileContentLoading(@NotNull ID<?, ?> indexId) {
    return !myNotRequiringContentIndices.contains(indexId);
  }

  @Nullable
  private IndexableFileSet getIndexableSetForFile(VirtualFile file) {
    for (IndexableFileSet set : myIndexableSets) {
      if (set.isInSet(file)) {
        return set;
      }
    }
    return null;
  }

  private void doTransientStateChangeForFile(int fileId, @NotNull VirtualFile file) {
    waitUntilIndicesAreInitialized();
    if (!clearUpToDateStateForPsiIndicesOfUnsavedDocuments(file, IndexingStamp.getNontrivialFileIndexedStates(fileId))) {
      // change in persistent file
      clearUpToDateStateForPsiIndicesOfVirtualFile(file);
    }
  }

  private void doInvalidateIndicesForFile(int fileId, @NotNull VirtualFile file, boolean contentChanged) {
    waitUntilIndicesAreInitialized();
    cleanProcessedFlag(file);

    List<ID<?, ?>> nontrivialFileIndexedStates = IndexingStamp.getNontrivialFileIndexedStates(fileId);
    Collection<ID<?, ?>> fileIndexedStatesToUpdate = ContainerUtil.intersection(nontrivialFileIndexedStates, myRequiringContentIndices);

    if (contentChanged) {
      // only mark the file as outdated, reindex will be done lazily
      if (!fileIndexedStatesToUpdate.isEmpty()) {
        //noinspection ForLoopReplaceableByForEach
        for (int i = 0, size = nontrivialFileIndexedStates.size(); i < size; ++i) {
          final ID<?, ?> indexId = nontrivialFileIndexedStates.get(i);
          if (needsFileContentLoading(indexId)) {
            getIndex(indexId).resetIndexedStateForFile(fileId);
          }
        }

        // transient index value can depend on disk value because former is diff to latter
        removeTransientFileDataFromIndices(nontrivialFileIndexedStates, fileId, file);

        // the file is for sure not a dir and it was previously indexed by at least one index
        if (file.isValid()) {
          if(!isTooLarge(file)) myChangedFilesCollector.scheduleForUpdate(file);
          else myChangedFilesCollector.scheduleForUpdate(new DeletedVirtualFileStub((VirtualFileWithId)file));
        } else {
          LOG.info("Unexpected state in update:" + file);
        }
      }
    }
    else { // file was removed
      for (ID<?, ?> indexId : nontrivialFileIndexedStates) {
        if (myNotRequiringContentIndices.contains(indexId)) {
          updateSingleIndex(indexId, null, fileId, null);
        }
      }
      if(!fileIndexedStatesToUpdate.isEmpty()) {
        // its data should be (lazily) wiped for every index
        myChangedFilesCollector.scheduleForUpdate(new DeletedVirtualFileStub((VirtualFileWithId)file));
      } else {
        myChangedFilesCollector.removeScheduledFileFromUpdate(file); // no need to update it anymore
      }
    }
  }

  private void scheduleFileForIndexing(int fileId, @NotNull VirtualFile file, boolean contentChange) {
    // handle 'content-less' indices separately
    boolean fileIsDirectory = file.isDirectory();

    if (!contentChange) {
      FileContent fileContent = null;
      for (ID<?, ?> indexId : fileIsDirectory ? myIndicesForDirectories : myNotRequiringContentIndices) {
        if (getInputFilter(indexId).acceptInput(file)) {
          if (fileContent == null) {
            fileContent = new FileContentImpl(file);
          }
          updateSingleIndex(indexId, file, fileId, fileContent);
        }
      }
    }
    // For 'normal indices' schedule the file for update and reset stamps for all affected indices (there
    // can be client that used indices between before and after events, in such case indices are up to date due to force update
    // with old content)
    if (!fileIsDirectory) {
      if (!file.isValid() || isTooLarge(file)) {
        // large file might be scheduled for update in before event when its size was not large
        myChangedFilesCollector.removeScheduledFileFromUpdate(file);
      }
      else {
        int finalFileId = fileId;
        myFileTypeManager.freezeFileTypeTemporarilyIn(file, () -> {
          final List<ID<?, ?>> candidates = getAffectedIndexCandidates(file);

          boolean scheduleForUpdate = false;

          //noinspection ForLoopReplaceableByForEach
          for (int i = 0, size = candidates.size(); i < size; ++i) {
            final ID<?, ?> indexId = candidates.get(i);
            if (needsFileContentLoading(indexId) && getInputFilter(indexId).acceptInput(file)) {
              getIndex(indexId).resetIndexedStateForFile(finalFileId);
              scheduleForUpdate = true;
            }
          }

          if (scheduleForUpdate) {
            IndexingStamp.flushCache(finalFileId);
            myChangedFilesCollector.scheduleForUpdate(file);
          }
        });
      }
    }
  }

  private final class ChangedFilesCollector extends IndexedFilesListener {
    private final IntObjectMap<VirtualFile> myFilesToUpdate = ContainerUtil.createConcurrentIntObjectMap();
    private final AtomicInteger myProcessedEventIndex = new AtomicInteger();
    private final Phaser myWorkersFinishedSync = new Phaser() {
      @Override
      protected boolean onAdvance(int phase, int registeredParties) {
        return false;
      }
    };

    private final ExecutorService myVfsEventsExecutor = SequentialTaskExecutor.createSequentialApplicationPoolExecutor("FileBasedIndex Vfs Event Processor");
    private final AtomicInteger myScheduledVfsEventsWorkers = new AtomicInteger();

    @Override
    protected void buildIndicesForFileRecursively(@NotNull VirtualFile file, boolean contentChange) {
      cleanProcessedFlag(file);
      if (!contentChange) {
        myUpdatingFiles.incrementAndGet();
      }

      super.buildIndicesForFileRecursively(file, contentChange);

      if (!contentChange) {
        if (myUpdatingFiles.decrementAndGet() == 0) {
          myFilesModCount.incrementAndGet();
        }
      }
    }

    @Override
    protected void iterateIndexableFiles(@NotNull VirtualFile file, @NotNull ContentIterator iterator) {
      for (IndexableFileSet set : myIndexableSets) {
        if (set.isInSet(file)) {
          set.iterateIndexableFilesIn(file, iterator);
        }
      }
    }

    void scheduleForUpdate(VirtualFile file) {
      if (!(file instanceof DeletedVirtualFileStub)) {
        IndexableFileSet setForFile = getIndexableSetForFile(file);
        if (setForFile == null) {
          return;
        }
      }
      final int fileId = Math.abs(getIdMaskingNonIdBasedFile(file));
      final VirtualFile previousVirtualFile = myFilesToUpdate.put(fileId, file);

      if (previousVirtualFile instanceof DeletedVirtualFileStub &&
          !previousVirtualFile.equals(file)) {
        assert ((DeletedVirtualFileStub)previousVirtualFile).getOriginalFile().equals(file);
        ((DeletedVirtualFileStub)previousVirtualFile).setResurrected(true);
        myFilesToUpdate.put(fileId, previousVirtualFile);
      }
    }

    private void removeScheduledFileFromUpdate(VirtualFile file) {
      final int fileId = Math.abs(getIdMaskingNonIdBasedFile(file));
      final VirtualFile previousVirtualFile = myFilesToUpdate.remove(fileId);

      if (previousVirtualFile instanceof DeletedVirtualFileStub) {
        assert ((DeletedVirtualFileStub)previousVirtualFile).getOriginalFile().equals(file);
        ((DeletedVirtualFileStub)previousVirtualFile).setResurrected(false);
        myFilesToUpdate.put(fileId, previousVirtualFile);
      }
    }

    private void removeFileIdFromFilesScheduledForUpdate(int fileId) {
      myFilesToUpdate.remove(fileId);
    }

    Collection<VirtualFile> getAllFilesToUpdate() {
      ensureUpToDate();
      if (myFilesToUpdate.isEmpty()) {
        return Collections.emptyList();
      }
      return new ArrayList<>(myFilesToUpdate.values());
    }

    @Override
    @NotNull
    public AsyncFileListener.ChangeApplier prepareChange(@NotNull List<? extends VFileEvent> events) {
      boolean shouldCleanup = ContainerUtil.exists(events, this::memoryStorageCleaningNeeded);
      ChangeApplier superApplier = super.prepareChange(events);

      return new AsyncFileListener.ChangeApplier() {
        @Override
        public void beforeVfsChange() {
          if (shouldCleanup) {
            cleanupMemoryStorage(false);
          }
          superApplier.beforeVfsChange();
        }

        @Override
        public void afterVfsChange() {
          superApplier.afterVfsChange();
          if (myInitialized) ensureUpToDateAsync();
        }
      };
    }

    private boolean memoryStorageCleaningNeeded(VFileEvent event) {
      Object requestor = event.getRequestor();
      return requestor instanceof FileDocumentManager ||
          requestor instanceof PsiManager ||
          requestor == LocalHistory.VFS_EVENT_REQUESTOR;
    }

    boolean isScheduledForUpdate(VirtualFile file) {
      return myFilesToUpdate.containsKey(Math.abs(getIdMaskingNonIdBasedFile(file)));
    }

    void ensureUpToDate() {
      if (!isUpToDateCheckEnabled()) {
        return;
      }
      //assert ApplicationManager.getApplication().isReadAccessAllowed() || ShutDownTracker.isShutdownHookRunning();
      waitUntilIndicesAreInitialized();

      if (ApplicationManager.getApplication().isReadAccessAllowed()) {
        processFilesInReadAction();
      } else {
        processFilesInReadActionWithYieldingToWriteAction();
      }
    }

    void ensureUpToDateAsync() {
      if (getEventMerger().getApproximateChangesCount() >= 20 && myScheduledVfsEventsWorkers.compareAndSet(0,1)) {
        myVfsEventsExecutor.submit(this::scheduledEventProcessingInReadActionWithYieldingToWriteAction);

        if (Registry.is("try.starting.dumb.mode.where.many.files.changed")) {
          Runnable startDumbMode = () -> {
            for (Project project : ProjectManager.getInstance().getOpenProjects()) {
              DumbServiceImpl dumbService = DumbServiceImpl.getInstance(project);
              DumbModeTask task = FileBasedIndexProjectHandler.createChangedFilesIndexingTask(project);

              if (task != null) {
                dumbService.queueTask(task);
              }
            }
          };

          Application app = ApplicationManager.getApplication();
          if (!app.isHeadlessEnvironment()  /*avoid synchronous ensureUpToDate to prevent deadlock*/ &&
              app.isDispatchThread() &&
              !LaterInvocator.isInModalContext()) {
            startDumbMode.run();
          }
          else {
            app.invokeLater(startDumbMode, ModalityState.NON_MODAL);
          }
        }
      }
    }

    private void processFilesInReadAction() {
      assert ApplicationManager.getApplication().isReadAccessAllowed(); // no vfs events -> event processing code can finish

      int publishedEventIndex = getEventMerger().getPublishedEventIndex();
      int processedEventIndex = myProcessedEventIndex.get();
      if (processedEventIndex == publishedEventIndex) {
        return;
      }

      myWorkersFinishedSync.register();
      int phase = myWorkersFinishedSync.getPhase();
      try {
        getEventMerger().processChanges(info ->
          ConcurrencyUtil.withLock(myWriteLock, () -> {
            try {
              ProgressManager.getInstance().executeNonCancelableSection(() -> {
                int fileId = info.getFileId();
                VirtualFile file = info.getFile();
                if (info.isTransientStateChanged()) doTransientStateChangeForFile(fileId, file);
                if (info.isBeforeContentChanged()) FileBasedIndexImpl.this.doInvalidateIndicesForFile(fileId, file, true);
                if (info.isContentChanged()) scheduleFileForIndexing(fileId, file, true);
                if (info.isFileRemoved()) FileBasedIndexImpl.this.doInvalidateIndicesForFile(fileId, file, false);
                if (info.isFileAdded()) scheduleFileForIndexing(fileId, file, false);
              });
            }
            finally {
              IndexingStamp.flushCache(info.getFileId());
            }
            return true;
          })
        );
      }
      finally {
        myWorkersFinishedSync.arriveAndDeregister();
      }

      myWorkersFinishedSync.awaitAdvance(phase);

      if (getEventMerger().getPublishedEventIndex() == publishedEventIndex) {
        myProcessedEventIndex.compareAndSet(processedEventIndex, publishedEventIndex);
      }
    }

    private void processFilesInReadActionWithYieldingToWriteAction() {
      while (getEventMerger().hasChanges()) {
        if (!ProgressIndicatorUtils.runInReadActionWithWriteActionPriority(this::processFilesInReadAction)) {
          ProgressIndicatorUtils.yieldToPendingWriteActions();
        }
      }
    }

    private void scheduledEventProcessingInReadActionWithYieldingToWriteAction() {
      try {
        processFilesInReadActionWithYieldingToWriteAction();
      }
      finally {
        myScheduledVfsEventsWorkers.decrementAndGet();
      }
    }
  }

  private boolean clearUpToDateStateForPsiIndicesOfUnsavedDocuments(@NotNull VirtualFile file, Collection<? extends ID<?, ?>> affectedIndices) {
    if (!myUpToDateIndicesForUnsavedOrTransactedDocuments.isEmpty()) {
      myUpToDateIndicesForUnsavedOrTransactedDocuments.clear();
    }

    Document document = myFileDocumentManager.getCachedDocument(file);

    if (document != null && myFileDocumentManager.isDocumentUnsaved(document)) {   // will be reindexed in indexUnsavedDocuments
      myLastIndexedDocStamps.clearForDocument(document); // Q: non psi indices
      document.putUserData(ourFileContentKey, null);

      return true;
    }

    removeTransientFileDataFromIndices(ContainerUtil.intersection(affectedIndices, myPsiDependentIndices), getFileId(file), file);
    return false;
  }

  static int getIdMaskingNonIdBasedFile(@NotNull VirtualFile file) {
    return file instanceof VirtualFileWithId ?((VirtualFileWithId)file).getId() : IndexingStamp.INVALID_FILE_ID;
  }

  private class UnindexedFilesFinder implements CollectingContentIterator {
    private final List<VirtualFile> myFiles = new ArrayList<>();
    private final boolean myDoTraceForFilesToBeIndexed = LOG.isTraceEnabled();

    @NotNull
    @Override
    public List<VirtualFile> getFiles() {
      List<VirtualFile> files;
      synchronized (myFiles) {
        files = myFiles;
      }

      // When processing roots concurrently myFiles looses the local order of local vs archive files
      // If we process the roots in 2 threads we can just separate local vs archive
      // IMPORTANT: also remove duplicated file that can appear due to roots intersection
      BitSet usedFileIds = new BitSet(files.size());
      List<VirtualFile> localFileSystemFiles = new ArrayList<>(files.size() / 2);
      List<VirtualFile> archiveFiles = new ArrayList<>(files.size() / 2);

      for(VirtualFile file:files) {
        int fileId = ((VirtualFileWithId)file).getId();
        if (usedFileIds.get(fileId)) continue;
        usedFileIds.set(fileId);

        if (file.getFileSystem() instanceof LocalFileSystem) localFileSystemFiles.add(file);
        else archiveFiles.add(file);
      }

      localFileSystemFiles.addAll(archiveFiles);
      return localFileSystemFiles;
    }

    @Override
    public boolean processFile(@NotNull final VirtualFile file) {
      return ReadAction.compute(()->{
        if (!file.isValid()) {
          return true;
        }
        if (file instanceof VirtualFileSystemEntry && ((VirtualFileSystemEntry)file).isFileIndexed()) {
          return true;
        }

        if (!(file instanceof VirtualFileWithId)) {
          return true;
        }
        myFileTypeManager.freezeFileTypeTemporarilyIn(file, () -> {
          boolean isUptoDate = true;
          if (file.isDirectory() || !isTooLarge(file)) {
            final List<ID<?, ?>> affectedIndexCandidates = getAffectedIndexCandidates(file);
            //noinspection ForLoopReplaceableByForEach
            for (int i = 0, size = affectedIndexCandidates.size(); i < size; ++i) {
              final ID<?, ?> indexId = affectedIndexCandidates.get(i);
              try {
                if (needsFileContentLoading(indexId) && shouldIndexFile(file, indexId)) {
                  if (myDoTraceForFilesToBeIndexed) {
                    LOG.trace("Scheduling indexing of " + file + " by request of index " + indexId);
                  }
                  synchronized (myFiles) {
                    myFiles.add(file);
                  }
                  isUptoDate = false;
                  break;
                }
              }
              catch (RuntimeException e) {
                final Throwable cause = e.getCause();
                if (cause instanceof IOException || cause instanceof StorageException) {
                  LOG.info(e);
                  requestRebuild(indexId);
                }
                else {
                  throw e;
                }
              }
            }
          }
          FileContent fileContent = null;
          int inputId = Math.abs(getIdMaskingNonIdBasedFile(file));
          for (ID<?, ?> indexId : myNotRequiringContentIndices) {
            if (shouldIndexFile(file, indexId)) {
              isUptoDate = false;
              if (fileContent == null) {
                fileContent = new FileContentImpl(file);
              }
              updateSingleIndex(indexId, file, inputId, fileContent);
            }
          }
          IndexingStamp.flushCache(inputId);

          if (isUptoDate && file instanceof VirtualFileSystemEntry) {
            ((VirtualFileSystemEntry)file).setFileIndexed(true);
          }
        });

        ProgressManager.checkCanceled();
        return true;
      });
    }
  }

  private boolean shouldIndexFile(@NotNull VirtualFile file, @NotNull ID<?, ?> indexId) {
    return getInputFilter(indexId).acceptInput(file) &&
           (isMock(file) || !getIndex(indexId).isIndexedStateForFile(((NewVirtualFile)file).getId(), file));
  }

  static boolean isMock(final VirtualFile file) {
    return !(file instanceof NewVirtualFile);
  }

  private boolean isTooLarge(@NotNull VirtualFile file) {
    if (SingleRootFileViewProvider.isTooLargeForIntelligence(file)) {
      return !myNoLimitCheckTypes.contains(file.getFileType()) || SingleRootFileViewProvider.isTooLargeForContentLoading(file);
    }
    return false;
  }

  private boolean isTooLarge(@NotNull VirtualFile file, long contentSize) {
    if (SingleRootFileViewProvider.isTooLargeForIntelligence(file, contentSize)) {
      return !myNoLimitCheckTypes.contains(file.getFileType()) || SingleRootFileViewProvider.isTooLargeForContentLoading(file, contentSize);
    }
    return false;
  }

  @NotNull
  CollectingContentIterator createContentIterator(@Nullable ProgressIndicator indicator) {
    return new UnindexedFilesFinder();
  }

  @Override
  public void registerIndexableSet(@NotNull IndexableFileSet set, @Nullable Project project) {
    myIndexableSets.add(set);
    myIndexableSetToProjectMap.put(set, project);
    if (project != null) {
      ((PsiManagerImpl)PsiManager.getInstance(project)).addTreeChangePreprocessor(event -> {
        if (event.isGenericChange() &&
            event.getCode() == PsiTreeChangeEventImpl.PsiEventType.CHILDREN_CHANGED) {
          PsiFile file = event.getFile();

          if (file != null) {
            VirtualFile virtualFile = file.getVirtualFile();

            if (virtualFile instanceof VirtualFileWithId) {
              myChangedFilesCollector.getEventMerger().recordTransientStateChangeEvent(virtualFile);
            }
          }
        }
      });
    }
  }

  private void clearUpToDateStateForPsiIndicesOfVirtualFile(VirtualFile virtualFile) {
    if (virtualFile instanceof VirtualFileWithId) {
      int fileId = ((VirtualFileWithId)virtualFile).getId();
      boolean wasIndexed = false;
      List<ID<?, ?>> candidates = getAffectedIndexCandidates(virtualFile);
      for (ID<?, ?> candidate : candidates) {
        if (myPsiDependentIndices.contains(candidate)) {
          if(getInputFilter(candidate).acceptInput(virtualFile)) {
            getIndex(candidate).resetIndexedStateForFile(fileId);
            wasIndexed = true;
          }
        }
      }
      if (wasIndexed) {
        myChangedFilesCollector.scheduleForUpdate(virtualFile);
        IndexingStamp.flushCache(fileId);
      }
    }
  }

  @Override
  public void removeIndexableSet(@NotNull IndexableFileSet set) {
    if (!myIndexableSetToProjectMap.containsKey(set)) return;
    myIndexableSets.remove(set);
    myIndexableSetToProjectMap.remove(set);

    for (VirtualFile file : myChangedFilesCollector.getAllFilesToUpdate()) {
      final int fileId = Math.abs(getIdMaskingNonIdBasedFile(file));
      if (!file.isValid()) {
        removeDataFromIndicesForFile(fileId, file);
        myChangedFilesCollector.removeFileIdFromFilesScheduledForUpdate(fileId);
      }
      else if (getIndexableSetForFile(file) == null) { // todo remove data from indices for removed
        myChangedFilesCollector.removeFileIdFromFilesScheduledForUpdate(fileId);
      }
    }

    IndexingStamp.flushCaches();
  }

  @Override
  public VirtualFile findFileById(Project project, int id) {
    return IndexInfrastructure.findFileById((PersistentFS)ManagingFS.getInstance(), id);
  }

  @Nullable
  private static PsiFile findLatestKnownPsiForUncomittedDocument(@NotNull Document doc, @NotNull Project project) {
    return PsiDocumentManager.getInstance(project).getCachedPsiFile(doc);
  }

  private static void cleanupProcessedFlag() {
    final VirtualFile[] roots = ManagingFS.getInstance().getRoots();
    for (VirtualFile root : roots) {
      cleanProcessedFlag(root);
    }
  }

  private static void cleanProcessedFlag(@NotNull final VirtualFile file) {
    if (!(file instanceof VirtualFileSystemEntry)) return;

    final VirtualFileSystemEntry nvf = (VirtualFileSystemEntry)file;
    if (file.isDirectory()) {
      nvf.setFileIndexed(false);
      for (VirtualFile child : nvf.getCachedChildren()) {
        cleanProcessedFlag(child);
      }
    }
    else {
      nvf.setFileIndexed(false);
    }
  }

  @Override
  public void iterateIndexableFilesConcurrently(@NotNull ContentIterator processor, @NotNull Project project, @NotNull ProgressIndicator indicator) {
    PushedFilePropertiesUpdaterImpl.invokeConcurrentlyIfPossible(collectScanRootRunnables(processor, project, indicator));
  }

  @Override
  public void iterateIndexableFiles(@NotNull final ContentIterator processor, @NotNull final Project project, final ProgressIndicator indicator) {
    for(Runnable r: collectScanRootRunnables(processor, project, indicator)) r.run();
  }

  @NotNull
  private static List<Runnable> collectScanRootRunnables(@NotNull final ContentIterator processor,
                                                         @NotNull final Project project,
                                                         final ProgressIndicator indicator) {
    FileBasedIndexScanRunnableCollector collector = FileBasedIndexScanRunnableCollector.getInstance(project);
    return collector.collectScanRootRunnables(processor, indicator);
  }

  private final class DocumentUpdateTask extends UpdateTask<Document> {
    private final ID<?, ?> myIndexId;

    DocumentUpdateTask(ID<?, ?> indexId) {
      myIndexId = indexId;
    }

    @Override
    void doProcess(Document document, Project project) {
      indexUnsavedDocument(document, myIndexId, project, myFileDocumentManager.getFile(document));
    }
  }

  private class FileIndexDataInitialization extends IndexInfrastructure.DataInitialization<IndexConfiguration> {
    private final IndexConfiguration state = new IndexConfiguration();
    private final AtomicBoolean versionChanged = new AtomicBoolean();
    private boolean currentVersionCorrupted;
    private SerializationManagerEx mySerializationManagerEx;

    private void initAssociatedDataForExtensions() {
      long started = System.nanoTime();
      Iterator<FileBasedIndexExtension> extensions =
        IndexInfrastructure.hasIndices() ?
        ((ExtensionPointImpl<FileBasedIndexExtension>)FileBasedIndexExtension.EXTENSION_POINT_NAME.getPoint(null)).iterator() :
        Collections.emptyIterator();

      // todo: init contentless indices first ?
      while (extensions.hasNext()) {
        FileBasedIndexExtension<?, ?> extension = extensions.next();
        if (extension == null) break;
        ID<?, ?> name = extension.getName();
        RebuildStatus.registerIndex(name);

        myUnsavedDataUpdateTasks.put(name, new DocumentUpdateTask(name));

        if (!extension.dependsOnFileContent()) {
          if (extension.indexDirectories()) myIndicesForDirectories.add(name);
          myNotRequiringContentIndices.add(name);
        }
        else {
          myRequiringContentIndices.add(name);
        }

        if (isPsiDependentIndex(extension)) myPsiDependentIndices.add(name);
        myNoLimitCheckTypes.addAll(extension.getFileTypesWithSizeLimitNotApplicable());

        addNestedInitializationTask(() -> {
          try {
            versionChanged.compareAndSet(false, registerIndexer(extension, state));
          }
          catch (IOException io) {
            throw io;
          }
          catch (Throwable t) {
            PluginManager.handleComponentError(t, extension.getClass().getName(), null);
          }
        });
      }

      myExtensionsRelatedDataWasLoaded = true;
      LOG.info("File index extensions iterated:" + (System.nanoTime() - started) / 1000000);
    }

    @Override
    protected void prepare() {
      initAssociatedDataForExtensions();

      mySerializationManagerEx = SerializationManagerEx.getInstanceEx();
      File indexRoot = PathManager.getIndexRoot();

      PersistentIndicesConfiguration.loadConfiguration();

      final File corruptionMarker = new File(indexRoot, CORRUPTION_MARKER_NAME);
      currentVersionCorrupted = IndexInfrastructure.hasIndices() && corruptionMarker.exists();
      if (currentVersionCorrupted) {
        FileUtil.deleteWithRenaming(indexRoot);
        indexRoot.mkdirs();
        // serialization manager is initialized before and use removed index root so we need to reinitialize it
        mySerializationManagerEx.reinitializeNameStorage();
        ID.reinitializeDiskStorage();
        PersistentIndicesConfiguration.saveConfiguration();
        FileUtil.delete(corruptionMarker);
      }
    }

    @Override
    protected void onThrowable(@NotNull Throwable t) {
      LOG.error(t);
    }

    @Override
    protected IndexConfiguration finish() {
      try {
        state.finalizeFileTypeMappingForIndices();

        String rebuildNotification = null;
        if (currentVersionCorrupted) {
          rebuildNotification = "Index files on disk are corrupted. Indices will be rebuilt.";
        }
        else if (versionChanged.get()) {
          rebuildNotification = "Index file format has changed for some indices. These indices will be rebuilt.";
        }
        if (rebuildNotification != null
            && !ApplicationManager.getApplication().isHeadlessEnvironment()
            && Registry.is("ide.showIndexRebuildMessage")) {
          NOTIFICATIONS.createNotification("Index Rebuild", rebuildNotification, NotificationType.INFORMATION, null).notify(null);
        }

        state.freeze();
        myState = state; // memory barrier
        // check if rebuild was requested for any index during registration
        for (ID<?, ?> indexId : state.getIndexIDs()) {
          try {
            RebuildStatus.clearIndexIfNecessary(indexId, () -> clearIndex(indexId));
          }
          catch (StorageException e) {
            requestRebuild(indexId);
            LOG.error(e);
          }
        }

        registerIndexableSet(new AdditionalIndexableFileSet(), null);
        return state;
      }
      finally {
        ShutDownTracker.getInstance().registerShutdownTask(FileBasedIndexImpl.this::performShutdown);
        saveRegisteredIndicesAndDropUnregisteredOnes(state.getIndexIDs());

        myFlushingFuture = FlushingDaemon.everyFiveSeconds(new Runnable() {
          private int lastModCount;

          @Override
          public void run() {
            mySerializationManagerEx.flushNameStorage();

            int currentModCount = myLocalModCount.get();
            if (lastModCount == currentModCount) {
              flushAllIndices(lastModCount);
            }
            lastModCount = currentModCount;
          }
        });
        myAllIndicesInitializedFuture = IndexInfrastructure.submitGenesisTask(() -> {
          if (!myShutdownPerformed.get()) {
            myChangedFilesCollector.ensureUpToDateAsync();
          }
          return null;
        });
        myInitialized = true;  // this will ensure that all changes to component's state will be visible to other threads
      }
    }
  }

  @Override
  public void invalidateCaches() {
    File indexRoot = PathManager.getIndexRoot();
    LOG.info("Requesting explicit indices invalidation", new Throwable());
    try {
      final File corruptionMarker = new File(indexRoot, CORRUPTION_MARKER_NAME);
      //noinspection IOResourceOpenedButNotSafelyClosed
      new FileOutputStream(corruptionMarker).close();
    }
    catch (Throwable ignore) {
    }
  }

  @TestOnly
  public void waitForVfsEventsExecuted(long timeout, @NotNull TimeUnit unit) throws Exception {
    ApplicationManager.getApplication().assertIsDispatchThread();
    long deadline = System.nanoTime() + unit.toNanos(timeout);
    while (System.nanoTime() < deadline) {
      try {
        ((BoundedTaskExecutor)myChangedFilesCollector.myVfsEventsExecutor).waitAllTasksExecuted(100, TimeUnit.MILLISECONDS);
        return;
      }
      catch (TimeoutException e) {
        UIUtil.dispatchAllInvocationEvents();
      }
    }
  }

  private static final boolean INDICES_ARE_PSI_DEPENDENT_BY_DEFAULT = SystemProperties.getBooleanProperty("idea.indices.psi.dependent.default", true);
  public static boolean isPsiDependentIndex(@NotNull IndexExtension<?, ?, ?> extension) {
    if (INDICES_ARE_PSI_DEPENDENT_BY_DEFAULT) {
      return extension instanceof FileBasedIndexExtension &&
             ((FileBasedIndexExtension<?, ?>)extension).dependsOnFileContent() &&
             !(extension instanceof DocumentChangeDependentIndex);
    } else {
      return extension instanceof PsiDependentIndex;
    }
  }
}
