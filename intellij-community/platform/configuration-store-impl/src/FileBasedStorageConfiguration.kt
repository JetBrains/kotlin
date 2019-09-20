// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.configurationStore

import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile

interface FileBasedStorageConfiguration {
  val isUseVfsForRead: Boolean

  val isUseVfsForWrite: Boolean

  fun resolveVirtualFile(path: String): VirtualFile? {
    val result = LocalFileSystem.getInstance().refreshAndFindFileByPath(path)
    if (result == null || !result.isValid) {
      return null
    }

    // otherwise virtualFile.contentsToByteArray() will query expensive FileTypeManager.getInstance()).getByFile()
    result.setCharset(Charsets.UTF_8, null, false)
    return result
  }
}

internal val defaultFileBasedStorageConfiguration: FileBasedStorageConfiguration = object : FileBasedStorageConfiguration {
  override val isUseVfsForRead: Boolean
    get() = false

  override val isUseVfsForWrite: Boolean
    get() = true
}