/*
 * Copyright 2000-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.util.indexing;

import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.roots.ContentIterator;
import com.intellij.openapi.vfs.*;
import com.intellij.openapi.vfs.newvfs.ManagingFS;
import com.intellij.openapi.vfs.newvfs.NewVirtualFile;
import com.intellij.openapi.vfs.newvfs.events.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.List;

public abstract class IndexedFilesListener implements AsyncFileListener {
  private final ManagingFS myManagingFS = ManagingFS.getInstance();
  private final VfsEventsMerger myEventMerger = new VfsEventsMerger();
  @Nullable private final VirtualFile myConfig;
  @Nullable private final VirtualFile myLog;

  public IndexedFilesListener() {
    myConfig = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(new File(PathManager.getConfigPath()));
    myLog = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(new File(PathManager.getLogPath()));
  }

  protected VfsEventsMerger getEventMerger() {
    return myEventMerger;
  }

  protected void buildIndicesForFileRecursively(@NotNull final VirtualFile file, final boolean contentChange) {
    if (file.isDirectory()) {
      final ContentIterator iterator = fileOrDir -> {
        myEventMerger.recordFileEvent(fileOrDir, contentChange);
        return true;
      };

      iterateIndexableFiles(file, iterator);
    }
    else {
      myEventMerger.recordFileEvent(file, contentChange);
    }
  }

  private boolean invalidateIndicesForFile(@NotNull VirtualFile file, boolean contentChange, VfsEventsMerger eventMerger) {
    if (isUnderConfigOrSystem(file)) {
      return false;
    }
    ProgressManager.checkCanceled();
    eventMerger.recordBeforeFileEvent(file, contentChange);
    return !file.isDirectory() || FileBasedIndexImpl.isMock(file) || myManagingFS.wereChildrenAccessed(file);
  }

  protected abstract void iterateIndexableFiles(@NotNull VirtualFile file, @NotNull ContentIterator iterator);

  void invalidateIndicesRecursively(@NotNull VirtualFile file, boolean contentChange, VfsEventsMerger eventMerger) {
    VfsUtilCore.visitChildrenRecursively(file, new VirtualFileVisitor() {
      @Override
      public boolean visitFile(@NotNull VirtualFile file) {
        return invalidateIndicesForFile(file, contentChange, eventMerger);
      }

      @Override
      public Iterable<VirtualFile> getChildrenIterable(@NotNull VirtualFile file) {
        return file instanceof NewVirtualFile ? ((NewVirtualFile)file).iterInDbChildren() : null;
      }
    });
  }

  @Override
  @NotNull
  public ChangeApplier prepareChange(@NotNull List<? extends VFileEvent> events) {
    VfsEventsMerger tempMerger = new VfsEventsMerger();
    for (VFileEvent event : events) {
      if (event instanceof VFileContentChangeEvent) {
        invalidateIndicesRecursively(((VFileContentChangeEvent)event).getFile(), true, tempMerger);
      }
      else if (event instanceof VFileDeleteEvent) {
        invalidateIndicesRecursively(((VFileDeleteEvent)event).getFile(), false, tempMerger);
      }
      else if (event instanceof VFilePropertyChangeEvent) {
        final VFilePropertyChangeEvent pce = (VFilePropertyChangeEvent)event;
        String propertyName = pce.getPropertyName();
        if (propertyName.equals(VirtualFile.PROP_NAME)) {
          // indexes may depend on file name
          // name change may lead to filetype change so the file might become not indexable
          // in general case have to 'unindex' the file and index it again if needed after the name has been changed
          invalidateIndicesRecursively(pce.getFile(), false, tempMerger);
        } else if (propertyName.equals(VirtualFile.PROP_ENCODING)) {
          invalidateIndicesRecursively(pce.getFile(), true, tempMerger);
        }
      }
    }
    return new ChangeApplier() {
      @Override
      public void beforeVfsChange() {
        myEventMerger.applyMergedEvents(tempMerger);
      }

      @Override
      public void afterVfsChange() {
        processAfterEvents(events);
      }
    };
  }

  private void processAfterEvents(@NotNull List<? extends VFileEvent> events) {
    for (VFileEvent event : events) {
      if (event instanceof VFileContentChangeEvent) {
        buildIndicesForFileRecursively(((VFileContentChangeEvent)event).getFile(), true);
      }
      else if (event instanceof VFileCopyEvent) {
        final VFileCopyEvent ce = (VFileCopyEvent)event;
        final VirtualFile copy = ce.getNewParent().findChild(ce.getNewChildName());
        if (copy != null) {
          buildIndicesForFileRecursively(copy, false);
        }
      }
      else if (event instanceof VFileCreateEvent) {
        final VirtualFile newChild = event.getFile();
        if (newChild != null) {
          buildIndicesForFileRecursively(newChild, false);
        }
      }
      else if (event instanceof VFileMoveEvent) {
        buildIndicesForFileRecursively(((VFileMoveEvent)event).getFile(), false);
      }
      else if (event instanceof VFilePropertyChangeEvent) {
        final VFilePropertyChangeEvent pce = (VFilePropertyChangeEvent)event;
        String propertyName = pce.getPropertyName();
        if (propertyName.equals(VirtualFile.PROP_NAME)) {
          // indexes may depend on file name
          buildIndicesForFileRecursively(pce.getFile(), false);
        } else if (propertyName.equals(VirtualFile.PROP_ENCODING)) {
          buildIndicesForFileRecursively(pce.getFile(), true);
        }
      }
    }
  }

  private boolean isUnderConfigOrSystem(@NotNull VirtualFile file) {
    return myConfig != null && VfsUtilCore.isAncestor(myConfig, file, false) ||
           myLog != null && VfsUtilCore.isAncestor(myLog, file, false);
  }
}