// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.service.project.autoimport;

import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.vfs.AsyncFileListener;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileVisitor;
import com.intellij.openapi.vfs.newvfs.NewVirtualFile;
import com.intellij.openapi.vfs.newvfs.events.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class AsyncFileChangeListenerBase implements AsyncFileListener {
  protected abstract boolean isRelevant(String path);
  
  protected abstract void prepareFileDeletion(@NotNull VirtualFile file); 

  protected abstract void updateFile(@NotNull VirtualFile file, @NotNull VFileEvent event);

  protected abstract void reset();

  protected abstract void apply();

  @Nullable
  @Override
  public ChangeApplier prepareChange(@NotNull List<? extends VFileEvent> events) {
    List<VFileEvent> relevantEvents = new ArrayList<>();
    try {
      for (VFileEvent each : events) {
        ProgressManager.checkCanceled();

        if (each instanceof VFileDeleteEvent) {
          deleteRecursively(each.getFile());
        }
        else {
          if (!isRelevant(each.getPath())) continue;

          relevantEvents.add(each);

          if (each instanceof VFilePropertyChangeEvent && ((VFilePropertyChangeEvent)each).isRename()) {
            deleteRecursively(each.getFile());
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
        after(relevantEvents);
      }
    };
  }

  private void deleteRecursively(VirtualFile f) {
    VfsUtilCore.visitChildrenRecursively(f, new VirtualFileVisitor<Void>() {
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

  private void after(@NotNull List<? extends VFileEvent> relevantEvents) {
    for (VFileEvent each : relevantEvents) {
      if (each instanceof VFileCreateEvent) {
        VirtualFile newChild = each.getFile();
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
      else if (each instanceof VFileContentChangeEvent ||
               each instanceof VFileMoveEvent ||
               each instanceof VFilePropertyChangeEvent && ((VFilePropertyChangeEvent)each).isRename()) {
        updateFile(Objects.requireNonNull(each.getFile()), each);
      }
    }
    apply();
  }

}
