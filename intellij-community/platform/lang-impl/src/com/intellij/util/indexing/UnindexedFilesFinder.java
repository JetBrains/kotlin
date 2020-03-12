// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileFilter;
import com.intellij.openapi.vfs.VirtualFileWithId;
import com.intellij.openapi.vfs.newvfs.impl.VirtualFileSystemEntry;
import com.intellij.psi.search.FileTypeIndex;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

class UnindexedFilesFinder implements VirtualFileFilter {
  private static final Logger LOG = Logger.getInstance(UnindexedFilesFinder.class);

  private final Project myProject;
  private final boolean myDoTraceForFilesToBeIndexed = FileBasedIndexImpl.LOG.isTraceEnabled();
  private final FileBasedIndexImpl myFileBasedIndex;
  private final UpdatableIndex<FileType, Void, FileContent> myFileTypeIndex;
  private final Collection<FileBasedIndexInfrastructureExtension.FileIndexingStatusProcessor> myStateProcessors;

  UnindexedFilesFinder(@NotNull Project project,
                       @NotNull FileBasedIndexImpl fileBasedIndex) {
    myProject = project;
    myFileBasedIndex = fileBasedIndex;
    myFileTypeIndex = fileBasedIndex.getIndex(FileTypeIndex.NAME);

    myStateProcessors = FileBasedIndexInfrastructureExtension
      .EP_NAME
      .extensions()
      .map(ex -> ex.createFileIndexingStatusProcessor(project))
      .filter(Objects::nonNull)
      .collect(Collectors.toList());
  }

  @Override
  public boolean accept(VirtualFile file) {
    return ReadAction.compute(() -> {
      if (!file.isValid()
          || file instanceof VirtualFileSystemEntry && ((VirtualFileSystemEntry)file).isFileIndexed()
          || !(file instanceof VirtualFileWithId)
      ) {
        return false;
      }

      AtomicBoolean shouldIndexFile = new AtomicBoolean(false);
      FileBasedIndexImpl.getFileTypeManager().freezeFileTypeTemporarilyIn(file, () -> {
        IndexedFile fileContent = new IndexedFileImpl(file, myProject);

        boolean isDirectory = file.isDirectory();
        int inputId = Math.abs(FileBasedIndexImpl.getIdMaskingNonIdBasedFile(file));
        if (!isDirectory && !myFileBasedIndex.isTooLarge(file)) {

          if (!myFileTypeIndex.isIndexedStateForFile(inputId, fileContent)) {
            myFileBasedIndex.dropNontrivialIndexedStates(inputId);
            shouldIndexFile.set(true);
          } else {
            final List<ID<?, ?>> affectedIndexCandidates = myFileBasedIndex.getAffectedIndexCandidates(file);
            //noinspection ForLoopReplaceableByForEach
            for (int i = 0, size = affectedIndexCandidates.size(); i < size; ++i) {
              final ID<?, ?> indexId = affectedIndexCandidates.get(i);
              try {
                if (myFileBasedIndex.needsFileContentLoading(indexId)) {
                  FileBasedIndexImpl.FileIndexingState fileIndexingState = myFileBasedIndex.shouldIndexFile(fileContent, indexId);
                  if (fileIndexingState == FileBasedIndexImpl.FileIndexingState.UP_TO_DATE) {
                    myStateProcessors.forEach(p -> p.processUpToDateFile(file, inputId, indexId));
                  } else if (fileIndexingState == FileBasedIndexImpl.FileIndexingState.SHOULD_INDEX) {
                    if (myDoTraceForFilesToBeIndexed) {
                      LOG.trace("Scheduling indexing of " + file + " by request of index " + indexId);
                    }
                    shouldIndexFile.set(true);
                    break;
                  }
                }
              }
              catch (RuntimeException e) {
                final Throwable cause = e.getCause();
                if (cause instanceof IOException || cause instanceof StorageException) {
                  LOG.info(e);
                  myFileBasedIndex.requestRebuild(indexId);
                }
                else {
                  throw e;
                }
              }
            }
          }
        }

        for (ID<?, ?> indexId : myFileBasedIndex.getContentLessIndexes(isDirectory)) {
          if (myFileBasedIndex.shouldIndexFile(fileContent, indexId) == FileBasedIndexImpl.FileIndexingState.SHOULD_INDEX) {
            myFileBasedIndex.updateSingleIndex(indexId, file, inputId, new IndexedFileWrapper(fileContent));
          }
        }
        IndexingStamp.flushCache(inputId);

        if (!shouldIndexFile.get() && file instanceof VirtualFileSystemEntry) {
          ((VirtualFileSystemEntry)file).setFileIndexed(true);
        }
      });
      return shouldIndexFile.get();
    });
  }
}