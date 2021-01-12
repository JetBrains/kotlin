// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.configurationStore

import com.intellij.openapi.components.StateStorageOperation
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile

interface FileBasedStorageConfiguration {
  val isUseVfsForRead: Boolean

  val isUseVfsForWrite: Boolean

  fun resolveVirtualFile(path: String, reasonOperation: StateStorageOperation) = doResolveVirtualFile(path, reasonOperation)
}

internal val defaultFileBasedStorageConfiguration = object : FileBasedStorageConfiguration {
  override val isUseVfsForRead: Boolean
    get() = false

  override val isUseVfsForWrite: Boolean
    get() = true
}

internal fun doResolveVirtualFile(path: String, reasonOperation: StateStorageOperation): VirtualFile? {
  val fs = LocalFileSystem.getInstance()
  val result = if (reasonOperation == StateStorageOperation.READ) fs.findFileByPath(path) else fs.refreshAndFindFileByPath(path)
  if (result == null || !result.isValid) {
    return null
  }

  // otherwise virtualFile.contentsToByteArray() will query expensive FileTypeManager.getInstance()).getByFile()
  result.setCharset(Charsets.UTF_8, null, false)
  return result
}