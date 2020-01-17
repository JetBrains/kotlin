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

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileTypes.FileTypeEvent;
import com.intellij.openapi.fileTypes.FileTypeListener;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.VirtualFileWithId;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.util.Consumer;
import com.intellij.util.containers.ConcurrentBitSet;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.messages.MessageBusConnection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * This is a light version of DirectoryIndexImpl
 *
 * @author gregsh
 */
public final class LightDirectoryIndex<T> {
  private final Map<VirtualFile, T> myRootInfos = ContainerUtil.newConcurrentMap();
  private final ConcurrentBitSet myNonInterestingIds = new ConcurrentBitSet();
  private final T myDefValue;
  private final Consumer<LightDirectoryIndex<T>> myInitializer;

  public LightDirectoryIndex(@NotNull Disposable parentDisposable, @NotNull T defValue, @NotNull Consumer<LightDirectoryIndex<T>> initializer) {
    myDefValue = defValue;
    myInitializer = initializer;
    resetIndex();
    MessageBusConnection connection = ApplicationManager.getApplication().getMessageBus().connect(parentDisposable);
    connection.subscribe(FileTypeManager.TOPIC, new FileTypeListener() {
      @Override
      public void fileTypesChanged(@NotNull FileTypeEvent event) {
        resetIndex();
      }
    });

    connection.subscribe(VirtualFileManager.VFS_CHANGES, new BulkFileListener() {
      @Override
      public void after(@NotNull List<? extends VFileEvent> events) {
        for (VFileEvent event : events) {
          if (shouldReset(event)) {
            resetIndex();
            break;
          }
        }
      }
    });
  }

  private static boolean shouldReset(VFileEvent event) {
    if (event instanceof VFileCreateEvent) {
      return ((VFileCreateEvent)event).isDirectory();
    }
    VirtualFile file = event.getFile();
    return file == null || file.isDirectory();
  }

  public void resetIndex() {
    myRootInfos.clear();
    myNonInterestingIds.clear();
    myInitializer.consume(this);
  }

  public void putInfo(@Nullable VirtualFile file, @NotNull T value) {
    if (!(file instanceof VirtualFileWithId)) return;
    myRootInfos.put(file, value);
  }

  @NotNull
  public T getInfoForFile(@Nullable VirtualFile file) {
    if (!(file instanceof VirtualFileWithId) || !file.isValid()) return myDefValue;
    
    for (VirtualFile each = file; each != null; each = each.getParent()) {
      int id = ((VirtualFileWithId)each).getId();

      if (!myNonInterestingIds.get(id)) {
        T info = myRootInfos.get(each);
        if (info != null) {
          return info;
        }
        myNonInterestingIds.set(id);
      }
    }
    return myDefValue;
  }

}
