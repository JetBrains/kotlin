// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.service.project.autoimport;

import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.vfs.AsyncFileListener;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileVisitor;
import com.intellij.openapi.vfs.newvfs.NewVirtualFile;
import com.intellij.openapi.vfs.newvfs.events.*;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public abstract class AsyncFileChangeListenerBase implements AsyncFileListener {
  protected abstract boolean isRelevant(String path);
  
  protected abstract void prepareFileDeletion(@NotNull VirtualFile file); 

  protected abstract void updateFile(VirtualFile file, VFileEvent event);

  protected abstract void reset();

  protected abstract void apply();

  @Override
  public boolean needsReadAction() {
    return true;
  }

  @Nullable
  @Override
  public ChangeApplier prepareChange(@NotNull List<? extends VFileEvent> events) {
    try {
      for (VFileEvent each : events) {
        ProgressManager.checkCanceled();

        if (each instanceof VFileDeleteEvent) {
          deleteRecursively(each.getFile());
        }
        else {
          if (!isRelevant(each.getPath())) continue;
          if (each instanceof VFilePropertyChangeEvent) {
            if (isRenamed(each)) {
              deleteRecursively(each.getFile());
            }
          }
          else if (each instanceof VFileMoveEvent) {
            VFileMoveEvent moveEvent = (VFileMoveEvent)each;
            String newPath = moveEvent.getNewParent().getPath() + "/" + moveEvent.getFile().getName();
            if (!isRelevant(newPath)) {
              deleteRecursively(moveEvent.getFile());
            }
          }
        }
      }
    }
    catch (ProcessCanceledException e) {
      reset();
      throw e;
    }

    return new ChangeApplier() {
      @Override
      public void beforeVfsChange() {
        apply();
      }

      @Override
      public void afterVfsChange() {
        after(events);
      }
    };
  }

  private void deleteRecursively(VirtualFile f) {
    VfsUtilCore.visitChildrenRecursively(f, new VirtualFileVisitor() {
      @Override
      public boolean visitFile(@NotNull VirtualFile f) {
        if (isRelevant(f.getPath())) {
          prepareFileDeletion(f);
        }
        return true;
      }

      @Nullable
      @Override
      public Iterable<VirtualFile> getChildrenIterable(@NotNull VirtualFile f) {
        return f.isDirectory() && f instanceof NewVirtualFile ? ((NewVirtualFile)f).iterInDbChildren() : null;
      }
    });
  }

  private void after(@NotNull List<? extends VFileEvent> events) {
    for (VFileEvent each : events) {
      if (!isRelevant(each.getPath())) continue;

      if (each instanceof VFileCreateEvent) {
        VFileCreateEvent createEvent = (VFileCreateEvent)each;
        VirtualFile newChild = createEvent.getParent().findChild(createEvent.getChildName());
        if (newChild != null) {
          updateFile(newChild, each);
        }
      }
      else if (each instanceof VFileCopyEvent) {
        VFileCopyEvent copyEvent = (VFileCopyEvent)each;
        VirtualFile newChild = copyEvent.getNewParent().findChild(copyEvent.getNewChildName());
        if (newChild != null) {
          updateFile(newChild, each);
        }
      }
      else if (each instanceof VFileContentChangeEvent) {
        updateFile(each.getFile(), each);
      }
      else if (each instanceof VFilePropertyChangeEvent) {
        if (isRenamed(each)) {
          updateFile(each.getFile(), each);
        }
      }
      else if (each instanceof VFileMoveEvent) {
        updateFile(each.getFile(), each);
      }
    }
    apply();
  }

  @Contract(pure=true)
  private static boolean isRenamed(VFileEvent each) {
    return ((VFilePropertyChangeEvent)each).getPropertyName().equals(VirtualFile.PROP_NAME)
           && !Comparing.equal(((VFilePropertyChangeEvent)each).getOldValue(), ((VFilePropertyChangeEvent)each).getNewValue());
  }
}
