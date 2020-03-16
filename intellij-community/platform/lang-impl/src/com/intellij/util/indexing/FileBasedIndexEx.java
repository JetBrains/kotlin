// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing;

import com.intellij.ide.lightEdit.LightEdit;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileWithId;
import com.intellij.openapi.vfs.newvfs.ManagingFS;
import com.intellij.openapi.vfs.newvfs.persistent.PersistentFS;
import com.intellij.psi.search.EverythingGlobalScope;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.*;
import com.intellij.util.containers.ConcurrentBitSet;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.indexing.impl.InvertedIndexValueIterator;
import com.intellij.util.indexing.roots.*;
import gnu.trove.THashSet;
import gnu.trove.TIntHashSet;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;
import java.util.function.IntPredicate;

import static com.intellij.util.indexing.FileBasedIndexImpl.getCauseToRebuildIndex;

@ApiStatus.Internal
public abstract class FileBasedIndexEx extends FileBasedIndex {
  private final IndexAccessValidator myAccessValidator = new IndexAccessValidator();

  @ApiStatus.Internal
  @NotNull
  public abstract IntPredicate getAccessibleFileIdFilter(@Nullable Project project);

  @ApiStatus.Internal
  public abstract ProjectIndexableFilesFilter projectIndexableFiles(@Nullable Project project);

  @ApiStatus.Internal
  abstract <K, V> UpdatableIndex<K, V, FileContent> getIndex(ID<K, V> indexId);

  @ApiStatus.Internal
  public abstract void waitUntilIndicesAreInitialized();

  /**
   * @return true if index can be processed after it or
   * false if no need to process it because, for example, scope is empty or index is going to rebuild.
   */
  @ApiStatus.Internal
  public abstract <K> boolean ensureUpToDate(@NotNull final ID<K, ?> indexId,
                                             @Nullable Project project,
                                             @Nullable GlobalSearchScope filter,
                                             @Nullable VirtualFile restrictedFile);

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
      if (!ensureUpToDate(indexId, scope.getProject(), scope, null)) {
        return true;
      }
      if (idFilter == null) {
        idFilter = projectIndexableFiles(scope.getProject());
      }
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
    if (!(virtualFile instanceof VirtualFileWithId)) return Collections.emptyMap();
    int fileId = getFileId(virtualFile);

    // TODO revise behaviour later
    if (getAccessibleFileIdFilter(project).test(fileId)) {
      Map<K, V> map = processExceptions(id, virtualFile, GlobalSearchScope.fileScope(project, virtualFile), index -> index.getIndexedFileData(fileId));
      return ContainerUtil.notNullize(map);
    }
    return Collections.emptyMap();
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

  @Override
  public <K, V> long getIndexModificationStamp(@NotNull ID<K, V> indexId, @NotNull Project project) {
    UpdatableIndex<K, V, FileContent> index = getIndex(indexId);
    ensureUpToDate(indexId, project, GlobalSearchScope.allScope(project));
    return index.getModificationStamp();
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
      if (!ensureUpToDate(indexId, project, filter, restrictToFile)) {
        return null;
      }

      return myAccessValidator.validate(indexId, ()-> ConcurrencyUtil.withLock(index.getReadLock(), ()->computable.convert(index)));
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

    if (!getAccessibleFileIdFilter(scope.getProject()).test(restrictedFileId)) return true;

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
    IntPredicate accessibleFileFilter = getAccessibleFileIdFilter(scope.getProject());

    return processValueIterator(indexId, dataKey, null, scope, valueIt -> {
      while (valueIt.hasNext()) {
        final V value = valueIt.next();
        for (final ValueContainer.IntIterator inputIdsIterator = valueIt.getInputIdsIterator(); inputIdsIterator.hasNext(); ) {
          final int id = inputIdsIterator.next();
          if (!accessibleFileFilter.test(id) || (filter != null && !filter.containsFileId(id))) continue;
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

  /**
   * DO NOT CALL DIRECTLY IN CLIENT CODE
   * The method is internal to indexing engine end is called internally. The method is public due to implementation details
   */
  @Override
  public <K> void ensureUpToDate(@NotNull final ID<K, ?> indexId, @Nullable Project project, @Nullable GlobalSearchScope filter) {
    waitUntilIndicesAreInitialized();
    ensureUpToDate(indexId, project, filter, null);
  }

  @Override
  public void iterateIndexableFiles(@NotNull ContentIterator processor, @NotNull Project project, @Nullable ProgressIndicator indicator) {
    final ProgressIndicator finalIndicator = indicator == null ? new EmptyProgressIndicator() : indicator;
    List<IndexableFilesProvider> providers = getOrderedIndexableFilesProviders(project, finalIndicator);
    ConcurrentBitSet visitedFileSet = new ConcurrentBitSet();
    for (IndexableFilesProvider provider : providers) {
      if (!provider.iterateFiles(project, processor, visitedFileSet)) {
        break;
      }
    }
  }

  /**
   * Returns providers of files to be indexed. Indexing is performed in the order corresponding to the resulting list.
   */
  @NotNull
  public List<IndexableFilesProvider> getOrderedIndexableFilesProviders(@NotNull Project project,
                                                                        @NotNull ProgressIndicator indicator) {
    if (LightEdit.owns(project)) {
      return Collections.emptyList();
    }
    return ReadAction.compute(() -> {
      if (project.isDisposed()) {
        return Collections.emptyList();
      }

      Set<Library> seenLibraries = new HashSet<>();
      Set<Sdk> seenSdks = new HashSet<>();
      Set<OrderEntry> allEntries = new THashSet<>();

      List<IndexableFilesProvider> providers = new ArrayList<>();
      Module[] modules = ModuleManager.getInstance(project).getSortedModules();
      for (Module module : modules) {
        providers.add(new ModuleIndexableFilesProvider(module));

        OrderEntry[] orderEntries = ModuleRootManager.getInstance(module).getOrderEntries();
        for (OrderEntry orderEntry : orderEntries) {
          allEntries.add(orderEntry);
          if (orderEntry instanceof LibraryOrderEntry) {
            Library library = ((LibraryOrderEntry)orderEntry).getLibrary();
            if (library != null && seenLibraries.add(library)) {
              providers.add(new LibraryIndexableFilesProvider(library));
            }
          }
          if (orderEntry instanceof JdkOrderEntry) {
            Sdk sdk = ((JdkOrderEntry)orderEntry).getJdk();
            if (sdk != null && seenSdks.add(sdk)) {
              providers.add(new SdkIndexableFilesProvider(sdk));
            }
          }
        }
      }

      for (IndexableSetContributor contributor : IndexableSetContributor.EP_NAME.getExtensionList()) {
        providers.add(new IndexableSetContributorFilesProvider(contributor));
      }

      for (AdditionalLibraryRootsProvider provider : AdditionalLibraryRootsProvider.EP_NAME.getExtensionList()) {
        for (SyntheticLibrary library : provider.getAdditionalProjectLibraries(project)) {
          providers.add(new SyntheticLibraryIndexableFilesProvider(library));
        }
      }

      FileBasedIndexInfrastructureExtension.EP_NAME.extensions().forEach(ex -> ex.processProjectEntries(project, allEntries, indicator));

      return providers;
    });
  }

  @Nullable
  private <K, V> TIntHashSet collectFileIdsContainingAllKeys(@NotNull final ID<K, V> indexId,
                                                             @NotNull final Collection<? extends K> dataKeys,
                                                             @NotNull final GlobalSearchScope filter,
                                                             @Nullable final Condition<? super V> valueChecker,
                                                             @Nullable final ProjectIndexableFilesFilter projectFilesFilter) {
    IntPredicate accessibleFileFilter = getAccessibleFileIdFilter(filter.getProject());
    ThrowableConvertor<UpdatableIndex<K, V, FileContent>, TIntHashSet, StorageException> convertor =
      index -> InvertedIndexUtil.collectInputIdsContainingAllKeys(index, dataKeys, __ -> {
                                                                    ProgressManager.checkCanceled();
                                                                    return true;
                                                                  }, valueChecker,
                                                                  projectFilesFilter == null ? accessibleFileFilter::test : id -> {
                                                                    return projectFilesFilter.containsFileId(id) && accessibleFileFilter.test(id);
                                                                  });

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
}
