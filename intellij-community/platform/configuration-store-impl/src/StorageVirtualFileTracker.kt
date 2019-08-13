// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.configurationStore

import com.intellij.openapi.components.ComponentManager
import com.intellij.openapi.components.StateStorage
import com.intellij.openapi.components.impl.stores.FileStorageCoreUtil
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.*
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.messages.MessageBus
import java.nio.file.Paths
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.atomic.AtomicBoolean

class StorageVirtualFileTracker(private val messageBus: MessageBus) {
  private val filePathToStorage: ConcurrentMap<String, TrackedStorage> = ContainerUtil.newConcurrentMap()
  @Volatile
  private var hasDirectoryBasedStorages = false

  private val vfsListenerAdded = AtomicBoolean()

  interface TrackedStorage : StateStorage {
    val storageManager: StateStorageManagerImpl
  }

  fun put(path: String, storage: TrackedStorage) {
    filePathToStorage.put(path, storage)
    if (storage is DirectoryBasedStorage) {
      hasDirectoryBasedStorages = true
    }

    if (vfsListenerAdded.compareAndSet(false, true)) {
      addVfsChangesListener()
    }
  }

  fun remove(path: String) {
    filePathToStorage.remove(path)
  }

  fun remove(processor: (TrackedStorage) -> Boolean) {
    val iterator = filePathToStorage.values.iterator()
    for (storage in iterator) {
      if (processor(storage)) {
        iterator.remove()
      }
    }
  }

  private fun addVfsChangesListener() {
    messageBus.connect().subscribe(VirtualFileManager.VFS_CHANGES, object : BulkFileListener {
      override fun after(events: MutableList<out VFileEvent>) {
        var storageEvents: LinkedHashMap<ComponentManager, LinkedHashSet<StateStorage>>? = null
        eventLoop@ for (event in events) {
          var storage: StateStorage?
          if (event is VFilePropertyChangeEvent && VirtualFile.PROP_NAME == event.propertyName) {
            val oldPath = event.oldPath
            storage = filePathToStorage.remove(oldPath)
            if (storage != null) {
              filePathToStorage.put(event.path, storage)
              if (storage is FileBasedStorage) {
                storage.setFile(null, Paths.get(event.path))
              }
              // we don't support DirectoryBasedStorage renaming

              // StoragePathMacros.MODULE_FILE -> old path, we must update value
              storage.storageManager.pathRenamed(oldPath, event.path, event)
            }
          }
          else {
            val path = event.path
            storage = filePathToStorage.get(path)
            // we don't care about parent directory create (because it doesn't affect anything) and move (because it is not supported case),
            // but we should detect deletion - but again, it is not supported case. So, we don't check if some of registered storages located inside changed directory.

            // but if we have DirectoryBasedStorage, we check - if file located inside it
            if (storage == null && hasDirectoryBasedStorages && path.endsWith(FileStorageCoreUtil.DEFAULT_EXT, ignoreCase = true)) {
              storage = filePathToStorage.get(VfsUtil.getParentDir(path))
            }
          }

          if (storage == null) {
            continue
          }

          when (event) {
            is VFileMoveEvent -> {
              if (storage is FileBasedStorage) {
                storage.setFile(null, Paths.get(event.path))
              }
            }
            is VFileCreateEvent -> {
              if (storage is FileBasedStorage && event.requestor !is SaveSession) {
                storage.setFile(event.file, null)
              }
            }
            is VFileDeleteEvent -> {
              if (storage is FileBasedStorage) {
                storage.setFile(null, null)
              }
              else {
                (storage as DirectoryBasedStorage).setVirtualDir(null)
              }
            }
            is VFileCopyEvent -> continue@eventLoop
          }

          if (isFireStorageFileChangedEvent(event)) {
            val componentManager = storage.storageManager.componentManager!!
            if (storageEvents == null) {
              storageEvents = LinkedHashMap()
            }
            storageEvents.getOrPut(componentManager) { LinkedHashSet() }.add(storage)
          }
        }

        if (storageEvents != null) {
          StoreReloadManager.getInstance().storageFilesChanged(storageEvents)
        }
      }
    })
  }
}