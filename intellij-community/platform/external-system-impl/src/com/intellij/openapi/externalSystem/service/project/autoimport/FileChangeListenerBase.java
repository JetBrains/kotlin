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
package com.intellij.openapi.externalSystem.service.project.autoimport;

import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileVisitor;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.NewVirtualFile;
import com.intellij.openapi.vfs.newvfs.events.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public abstract class FileChangeListenerBase implements BulkFileListener {
  protected abstract boolean isRelevant(String path);

  protected abstract void updateFile(VirtualFile file, VFileEvent event);

  protected abstract void deleteFile(VirtualFile file, VFileEvent event);

  protected abstract void apply();

  @Override
  public void before(@NotNull List<? extends VFileEvent> events) {
    for (VFileEvent each : events) {
      if (each instanceof VFileDeleteEvent) {
        deleteRecursively(each.getFile(), each);
      }
      else {
        if (!isRelevant(each.getPath())) continue;
        if (each instanceof VFilePropertyChangeEvent) {
          if (isRenamed(each)) {
            deleteRecursively(each.getFile(), each);
          }
        }
        else if (each instanceof VFileMoveEvent) {
          VFileMoveEvent moveEvent = (VFileMoveEvent)each;
          String newPath = moveEvent.getNewParent().getPath() + "/" + moveEvent.getFile().getName();
          if (!isRelevant(newPath)) {
            deleteRecursively(moveEvent.getFile(), each);
          }
        }
      }
    }
  }

  private void deleteRecursively(VirtualFile f, final VFileEvent event) {
    VfsUtilCore.visitChildrenRecursively(f, new VirtualFileVisitor() {
      @Override
      public boolean visitFile(@NotNull VirtualFile f) {
        if (isRelevant(f.getPath())) deleteFile(f, event);
        return true;
      }

      @Nullable
      @Override
      public Iterable<VirtualFile> getChildrenIterable(@NotNull VirtualFile f) {
        return f.isDirectory() && f instanceof NewVirtualFile ? ((NewVirtualFile)f).iterInDbChildren() : null;
      }
    });
  }

  @Override
  public void after(@NotNull List<? extends VFileEvent> events) {
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

  private static boolean isRenamed(VFileEvent each) {
    return ((VFilePropertyChangeEvent)each).getPropertyName().equals(VirtualFile.PROP_NAME)
           && !Comparing.equal(((VFilePropertyChangeEvent)each).getOldValue(), ((VFilePropertyChangeEvent)each).getNewValue());
  }
}
