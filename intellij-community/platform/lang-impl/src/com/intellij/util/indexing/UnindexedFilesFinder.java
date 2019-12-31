// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.CollectingContentIterator;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileWithId;
import com.intellij.openapi.vfs.newvfs.impl.VirtualFileSystemEntry;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Map;

class UnindexedFilesFinder implements CollectingContentIterator {
  private static final Logger LOG = Logger.getInstance(UnindexedFilesFinder.class);

  private final List<VirtualFile> myFiles = new ArrayList<>();
  private final Project myProject;
  private final boolean myDoTraceForFilesToBeIndexed = FileBasedIndexImpl.LOG.isTraceEnabled();
  private final FileBasedIndexImpl myFileBasedIndex;

  UnindexedFilesFinder(@NotNull Project project) {
    myProject = project;
    myFileBasedIndex = ((FileBasedIndexImpl)FileBasedIndex.getInstance());
  }

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
    return ReadAction.compute(() -> {
      if (!file.isValid()) {
        return true;
      }
      if (file instanceof VirtualFileSystemEntry && ((VirtualFileSystemEntry)file).isFileIndexed()) {
        return true;
      }

      if (!(file instanceof VirtualFileWithId)) {
        return true;
      }
      FileBasedIndexImpl.getFileTypeManager().freezeFileTypeTemporarilyIn(file, () -> {
        IndexedFile fileContent = new IndexedFileImpl(file, myProject);

        boolean isUptoDate = true;
        boolean isDirectory = file.isDirectory();
        int inputId = Math.abs(FileBasedIndexImpl.getIdMaskingNonIdBasedFile(file));
        if (!isDirectory && !myFileBasedIndex.isTooLarge(file)) {

          if (!isIndexedFileTypeUpToDate(fileContent, inputId)) {
            for (ID<?, ?> state : IndexingStamp.getNontrivialFileIndexedStates(inputId)) {
              myFileBasedIndex.getIndex(state).resetIndexedStateForFile(inputId);
            }
            synchronized (myFiles) {
              myFiles.add(file);
            }
            isUptoDate = false;
          }

          if (isUptoDate) {
            final List<ID<?, ?>> affectedIndexCandidates = myFileBasedIndex.getAffectedIndexCandidates(file);
            //noinspection ForLoopReplaceableByForEach
            for (int i = 0, size = affectedIndexCandidates.size(); i < size; ++i) {
              final ID<?, ?> indexId = affectedIndexCandidates.get(i);
              try {
                if (myFileBasedIndex.needsFileContentLoading(indexId) && myFileBasedIndex.shouldIndexFile(fileContent, indexId)) {
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
          if (myFileBasedIndex.shouldIndexFile(fileContent, indexId)) {
            myFileBasedIndex.updateSingleIndex(indexId, file, inputId, new IndexedFileWrapper(fileContent));
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

  private boolean isIndexedFileTypeUpToDate(@NotNull IndexedFile file, int inputId) {
    long stamp = IndexingStamp.getIndexStamp(inputId, FileTypeIndex.NAME);
    long fileTypeIndexVersion = IndexingStamp.getIndexCreationStamp(FileTypeIndex.NAME);
    if (stamp != fileTypeIndexVersion) return false;

    FileType actualFileType = file.getFileType();
    try {
      Map<FileType, Void> indexedFileType = myFileBasedIndex.getIndex(FileTypeIndex.NAME).getIndexedFileData(inputId);
      return actualFileType.equals(ContainerUtil.getFirstItem(indexedFileType.keySet()));
    }
    catch (StorageException e) {
      myFileBasedIndex.requestRebuild(FileTypeIndex.NAME, e);
      LOG.error(e);
      return false;
    }
  }
}