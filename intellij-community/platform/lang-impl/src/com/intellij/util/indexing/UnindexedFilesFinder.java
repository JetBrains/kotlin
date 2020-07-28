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
import com.intellij.util.containers.ContainerUtil;
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
  private final boolean myRunExtensionsForFilesMarkedAsIndexed;
  private final boolean myShouldProcessUpToDateFiles;

  UnindexedFilesFinder(@NotNull Project project,
                       @NotNull FileBasedIndexImpl fileBasedIndex,
                       boolean runExtensionsForFilesMarkedAsIndexed) {
    myProject = project;
    myFileBasedIndex = fileBasedIndex;
    myFileTypeIndex = fileBasedIndex.getIndex(FileTypeIndex.NAME);
    myRunExtensionsForFilesMarkedAsIndexed = runExtensionsForFilesMarkedAsIndexed;

    myStateProcessors = FileBasedIndexInfrastructureExtension
      .EP_NAME
      .extensions()
      .map(ex -> ex.createFileIndexingStatusProcessor(project))
      .filter(Objects::nonNull)
      .collect(Collectors.toList());
    myShouldProcessUpToDateFiles = ContainerUtil.find(myStateProcessors, p -> p.shouldProcessUpToDateFiles()) != null;
  }

  @Override
  public boolean accept(VirtualFile file) {
    return ReadAction.compute(() -> {
      if (myProject.isDisposed() || !file.isValid() || !(file instanceof VirtualFileWithId)) {
        return false;
      }
      else if (file instanceof VirtualFileSystemEntry && ((VirtualFileSystemEntry)file).isFileIndexed()) {
        if (myRunExtensionsForFilesMarkedAsIndexed && myShouldProcessUpToDateFiles) {
          int inputId = Math.abs(FileBasedIndexImpl.getIdMaskingNonIdBasedFile(file));
          List<ID<?, ?>> ids = IndexingStamp.getNontrivialFileIndexedStates(inputId);
          for (FileBasedIndexInfrastructureExtension.FileIndexingStatusProcessor processor : myStateProcessors) {
            for (ID<?, ?> id : ids) {
              if (myFileBasedIndex.needsFileContentLoading(id)) {
                processor.processUpToDateFile(file, inputId, id);
              }
            }
          }
        }
        return false;
      }

      AtomicBoolean shouldIndexFile = new AtomicBoolean(false);
      FileBasedIndexImpl.getFileTypeManager().freezeFileTypeTemporarilyIn(file, () -> {
        IndexedFile fileContent = new IndexedFileImpl(file, myProject);

        boolean isDirectory = file.isDirectory();
        int inputId = Math.abs(FileBasedIndexImpl.getIdMaskingNonIdBasedFile(file));
        FileIndexingState fileTypeIndexState = null;
        if (!isDirectory && !myFileBasedIndex.isTooLarge(file)) {
          if ((fileTypeIndexState = myFileTypeIndex.getIndexingStateForFile(inputId, fileContent)) == FileIndexingState.OUT_DATED) {
            myFileBasedIndex.dropNontrivialIndexedStates(inputId);
            shouldIndexFile.set(true);

          } else {
            final List<ID<?, ?>> affectedIndexCandidates = myFileBasedIndex.getAffectedIndexCandidates(file);
            //noinspection ForLoopReplaceableByForEach
            for (int i = 0, size = affectedIndexCandidates.size(); i < size; ++i) {
              final ID<?, ?> indexId = affectedIndexCandidates.get(i);
              try {
                if (myFileBasedIndex.needsFileContentLoading(indexId)) {
                  FileIndexingState fileIndexingState = myFileBasedIndex.shouldIndexFile(fileContent, indexId);
                  boolean indexInfrastructureExtensionInvalidated = false;
                  if (fileIndexingState == FileIndexingState.UP_TO_DATE) {
                    if (myShouldProcessUpToDateFiles) {
                      for (FileBasedIndexInfrastructureExtension.FileIndexingStatusProcessor p : myStateProcessors) {
                        if (!p.processUpToDateFile(file, inputId, indexId)) {
                          indexInfrastructureExtensionInvalidated = true;
                        }
                      }
                    }
                  }
                  if (indexInfrastructureExtensionInvalidated) {
                    fileIndexingState = myFileBasedIndex.shouldIndexFile(fileContent, indexId);
                  }
                  if (fileIndexingState.updateRequired()) {
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
          if (FileTypeIndex.NAME.equals(indexId) && fileTypeIndexState != null && !fileTypeIndexState.updateRequired()) {
            continue;
          }
          if (myFileBasedIndex.shouldIndexFile(fileContent, indexId).updateRequired()) {
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