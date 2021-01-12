// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.util.SmartList;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

class RegisteredIndexes {
  private static final Logger LOG = Logger.getInstance(RegisteredIndexes.class);

  @NotNull
  private final FileDocumentManager myFileDocumentManager;
  @NotNull
  private final FileBasedIndexImpl myFileBasedIndex;
  @NotNull
  private final Future<IndexConfiguration> myStateFuture;

  private final List<ID<?, ?>> myIndicesForDirectories = new SmartList<>();

  private final Set<ID<?, ?>> myNotRequiringContentIndices = new THashSet<>();
  private final Set<ID<?, ?>> myRequiringContentIndices = new THashSet<>();
  private final Set<ID<?, ?>> myPsiDependentIndices = new THashSet<>();
  private final Set<FileType> myNoLimitCheckTypes = new THashSet<>();

  private volatile boolean myExtensionsRelatedDataWasLoaded;

  private volatile boolean myInitialized;

  private volatile IndexConfiguration myState;
  private volatile Future<?> myAllIndicesInitializedFuture;

  private final Map<ID<?, ?>, DocumentUpdateTask> myUnsavedDataUpdateTasks = new ConcurrentHashMap<>();

  private final AtomicBoolean myShutdownPerformed = new AtomicBoolean(false);

  RegisteredIndexes(@NotNull FileDocumentManager fileDocumentManager,
                    @NotNull FileBasedIndexImpl fileBasedIndex) {
    myFileDocumentManager = fileDocumentManager;
    myFileBasedIndex = fileBasedIndex;
    myStateFuture = IndexInfrastructure.submitGenesisTask(new FileBasedIndexDataInitialization(fileBasedIndex, this));

    if (!IndexInfrastructure.ourDoAsyncIndicesInitialization) {
      waitUntilIndicesAreInitialized();
    }
  }

  boolean performShutdown() {
    return myShutdownPerformed.compareAndSet(false, true);
  }

  void setState(@NotNull IndexConfiguration state) {
    myState = state;
  }

  IndexConfiguration getState() {
    return myState;
  }

  IndexConfiguration getConfigurationState() {
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

  void waitUntilAllIndicesAreInitialized() {
    try {
      waitUntilIndicesAreInitialized();
      myAllIndicesInitializedFuture.get();
    } catch (Throwable ignore) {}
  }

  void waitUntilIndicesAreInitialized() {
    try {
      myStateFuture.get();
    }
    catch (Throwable t) {
      LOG.error(t);
    }
  }

  void extensionsDataWasLoaded() {
    myExtensionsRelatedDataWasLoaded = true;
  }

  void markInitialized() {
    myInitialized = true;
  }

  void ensureLoadedIndexesUpToDate() {
    myAllIndicesInitializedFuture = IndexInfrastructure.submitGenesisTask(() -> {
      if (!myShutdownPerformed.get()) {
        myFileBasedIndex.getChangedFilesCollector().ensureUpToDateAsync();
      }
      return null;
    });
  }

  void registerIndexExtension(@NotNull FileBasedIndexExtension<?, ?> extension) {
    ID<?, ?> name = extension.getName();
    myUnsavedDataUpdateTasks.put(name, new DocumentUpdateTask(name));

    if (!extension.dependsOnFileContent()) {
      if (extension.indexDirectories()) myIndicesForDirectories.add(name);
      myNotRequiringContentIndices.add(name);
    }
    else {
      myRequiringContentIndices.add(name);
    }

    if (FileBasedIndexImpl.isPsiDependentIndex(extension)) myPsiDependentIndices.add(name);
    myNoLimitCheckTypes.addAll(extension.getFileTypesWithSizeLimitNotApplicable());
  }

  @NotNull
  Set<FileType> getNoLimitCheckFileTypes() {
    return myNoLimitCheckTypes;
  }

  boolean areIndexesReady() {
    return myStateFuture.isDone() && myAllIndicesInitializedFuture.isDone();
  }

  boolean isExtensionsDataLoaded() {
    return myExtensionsRelatedDataWasLoaded;
  }

  boolean isInitialized() {
    return myInitialized;
  }

  Set<ID<?, ?>> getRequiringContentIndices() {
    return myRequiringContentIndices;
  }

  @NotNull
  Set<ID<?, ?>> getNotRequiringContentIndices() {
    return myNotRequiringContentIndices;
  }

  boolean isNotRequiringContentIndex(@NotNull ID<?, ?> indexId) {
    return myNotRequiringContentIndices.contains(indexId);
  }

  @NotNull
  List<ID<?, ?>> getIndicesForDirectories() {
    return myIndicesForDirectories;
  }

  Set<ID<?, ?>> getPsiDependentIndices() {
    return myPsiDependentIndices;
  }

  boolean isPsiDependentIndex(@NotNull ID<?, ?> indexId) {
    return myPsiDependentIndices.contains(indexId);
  }

  UpdateTask<Document> getUnsavedDataUpdateTask(@NotNull ID<?, ?> indexId) {
    return myUnsavedDataUpdateTasks.get(indexId);
  }

  private final class DocumentUpdateTask extends UpdateTask<Document> {
    private final ID<?, ?> myIndexId;

    DocumentUpdateTask(ID<?, ?> indexId) {
      myIndexId = indexId;
    }

    @Override
    void doProcess(Document document, Project project) {
      myFileBasedIndex.indexUnsavedDocument(document, myIndexId, project, myFileDocumentManager.getFile(document));
    }
  }
}
