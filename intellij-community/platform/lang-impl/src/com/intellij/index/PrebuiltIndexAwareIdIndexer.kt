// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.index

import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.impl.cache.impl.id.IdIndexEntry
import com.intellij.psi.impl.cache.impl.id.LexingIdIndexer
import com.intellij.util.indexing.FileContent
import com.intellij.util.io.DataExternalizer
import com.intellij.util.io.DataInputOutputUtil
import java.io.DataInput
import java.io.DataOutput

/**
 * @author traff
 */

abstract class PrebuiltIndexAwareIdIndexer : PrebuiltIndexProviderBase<Map<IdIndexEntry, Int>>(), LexingIdIndexer {
  companion object {
    private val LOG = Logger.getInstance("#com.intellij.index.PrebuiltIndexAwareIdIndexer")
    const val ID_INDEX_FILE_NAME: String = "id-index"
  }

  override val indexName: String get() = ID_INDEX_FILE_NAME

  override val indexExternalizer: IdIndexMapDataExternalizer get() = IdIndexMapDataExternalizer()

  override fun map(inputData: FileContent): Map<IdIndexEntry, Int> {
    val map = get(inputData)
    return if (map != null) {
      if (DEBUG_PREBUILT_INDICES) {
        if (map != idIndexMap(inputData)) {
          LOG.error("Prebuilt id index differs from actual value for ${inputData.file.path}")
        }
      }
      map
    }
    else {
      idIndexMap(inputData)
    }
  }

  abstract fun idIndexMap(inputData: FileContent): Map<IdIndexEntry, Int>
}

class IdIndexMapDataExternalizer : DataExternalizer<Map<IdIndexEntry, Int>> {
  override fun save(out: DataOutput, value: Map<IdIndexEntry, Int>) {
    DataInputOutputUtil.writeINT(out, value.size)
    for (e in value.entries) {
      DataInputOutputUtil.writeINT(out, e.key.wordHashCode)
      DataInputOutputUtil.writeINT(out, e.value)
    }
  }

  override fun read(`in`: DataInput): Map<IdIndexEntry, Int> {
    val size = DataInputOutputUtil.readINT(`in`)
    val map = HashMap<IdIndexEntry, Int>()
    for (i in 0 until size) {
      val wordHash = DataInputOutputUtil.readINT(`in`)
      val value = DataInputOutputUtil.readINT(`in`)
      map[IdIndexEntry(wordHash)] = value
    }
    return map
  }
}
