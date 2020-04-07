// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing

import com.intellij.openapi.util.Computable
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.Processor
import com.intellij.util.indexing.impl.AbstractUpdateData
import com.intellij.util.indexing.snapshot.EmptyValueContainer
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock

class EmptyUpdatableIndex<Key, Value, Input> : UpdatableIndex<Key, Value, Input> {
  private var lock = ReentrantReadWriteLock()

  @Throws(StorageException::class)
  override fun processAllKeys(processor: Processor<in Key>,
                              scope: GlobalSearchScope,
                              idFilter: IdFilter?): Boolean {
    return false
  }

  override fun getLock(): ReadWriteLock = lock

  @Throws(StorageException::class)
  override fun getIndexedFileData(fileId: Int): Map<Key, Value> = emptyMap()

  override fun setIndexedStateForFile(fileId: Int, file: IndexedFile) = Unit
  override fun resetIndexedStateForFile(fileId: Int) {}
  override fun getIndexingStateForFile(fileId: Int, file: IndexedFile): FileIndexingState = FileIndexingState.NOT_INDEXED

  override fun getModificationStamp(): Long = 0

  override fun removeTransientDataForFile(inputId: Int) = Unit
  override fun removeTransientDataForKeys(inputId: Int, keys: Collection<Key>) = Unit
  override fun getExtension(): IndexExtension<Key, Value, Input> = throw UnsupportedOperationException()

  @Throws(StorageException::class)
  override fun updateWithMap(updateData: AbstractUpdateData<Key, Value>) = Unit

  override fun setBufferingEnabled(enabled: Boolean) = Unit
  override fun cleanupMemoryStorage() = Unit
  override fun cleanupForNextTest() = Unit
  @Throws(StorageException::class)
  override fun getData(key: Key): ValueContainer<Value> {
    @Suppress("UNCHECKED_CAST")
    return EmptyValueContainer as ValueContainer<Value>
  }

  override fun update(inputId: Int, content: Input?): Computable<Boolean> = throw UnsupportedOperationException()

  @Throws(StorageException::class)
  override fun flush() {
  }

  @Throws(StorageException::class)
  override fun clear() {
  }

  override fun dispose() {}
}