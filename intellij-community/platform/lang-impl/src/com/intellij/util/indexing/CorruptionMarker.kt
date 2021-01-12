// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing

import com.intellij.openapi.application.PathManager
import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.stubs.SerializationManagerEx
import java.io.File
import java.io.FileOutputStream

internal object CorruptionMarker {
  private const val CORRUPTION_MARKER_NAME = "corruption.marker"

  @JvmStatic
  fun requestInvalidation() {
    val indexRoot = PathManager.getIndexRoot()
    FileBasedIndexImpl.LOG.info("Requesting explicit indices invalidation")
    try {
      val corruptionMarker = File(indexRoot, CORRUPTION_MARKER_NAME)
      FileOutputStream(corruptionMarker).close()
    }
    catch (ignore: Throwable) {
    }
  }

  @JvmStatic
  fun invalidateIndexesIfNeeded(): Boolean {
    val indexRoot = PathManager.getIndexRoot()
    val corruptionMarker = File(indexRoot, CORRUPTION_MARKER_NAME)
    val corrupted = IndexInfrastructure.hasIndices() && corruptionMarker.exists()
    if (corrupted) {
      FileUtil.deleteWithRenaming(indexRoot)
      indexRoot.mkdirs()
      // serialization manager is initialized before and use removed index root so we need to reinitialize it
      SerializationManagerEx.getInstanceEx().reinitializeNameStorage()
      ID.reinitializeDiskStorage()
      PersistentIndicesConfiguration.saveConfiguration()
      FileUtil.delete(corruptionMarker)
    }
    return corrupted
  }
}