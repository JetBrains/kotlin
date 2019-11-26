/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.Processor;
import com.intellij.util.indexing.impl.AbstractUpdateData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * @author Eugene Zhuravlev
 */
public interface UpdatableIndex<Key, Value, Input> extends InvertedIndex<Key,Value, Input> {

  boolean processAllKeys(@NotNull Processor<? super Key> processor, @NotNull GlobalSearchScope scope, @Nullable IdFilter idFilter) throws StorageException;

  @NotNull
  Lock getReadLock();

  @NotNull
  Lock getWriteLock();

  @NotNull
  ReadWriteLock getLock();

  @NotNull
  Map<Key, Value> getIndexedFileData(int fileId) throws StorageException;

  void setIndexedStateForFile(int fileId, @NotNull VirtualFile file);
  void resetIndexedStateForFile(int fileId);

  boolean isIndexedStateForFile(int fileId, @NotNull VirtualFile file);

  long getModificationStamp();

  void removeTransientDataForFile(int inputId);

  void removeTransientDataForKeys(int inputId, @NotNull Collection<? extends Key> keys);

  @NotNull
  IndexExtension<Key, Value, Input> getExtension();

  void updateWithMap(@NotNull AbstractUpdateData<Key, Value> updateData) throws StorageException;

  void setBufferingEnabled(boolean enabled);

  void cleanupMemoryStorage();

  @TestOnly
  void cleanupForNextTest();
}
