// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.configurationStore

import com.intellij.openapi.components.RoamingType
import com.intellij.util.io.*
import java.io.InputStream
import java.nio.file.NoSuchFileException
import java.nio.file.Path

class MockStreamProvider(private val dir: Path) : StreamProvider {
  override val isExclusive = true

  override fun write(fileSpec: String, content: ByteArray, size: Int, roamingType: RoamingType) {
    dir.resolve(fileSpec).write(content, 0, size)
  }

  override fun read(fileSpec: String, roamingType: RoamingType, consumer: (InputStream?) -> Unit): Boolean {
    val file = dir.resolve(fileSpec)
    try {
      file.inputStream().use(consumer)
    }
    catch (e: NoSuchFileException) {
      consumer(null)
    }
    return true
  }

  override fun processChildren(path: String, roamingType: RoamingType, filter: (name: String) -> Boolean, processor: (name: String, input: InputStream, readOnly: Boolean) -> Boolean): Boolean {
    dir.resolve(path).directoryStreamIfExists({ filter(it.fileName.toString()) }) { directoryStream ->
      for (file in directoryStream) {
        val attributes = file.basicAttributesIfExists()
        if (attributes == null || attributes.isDirectory || file.isHidden()) {
          continue
        }

        // we ignore empty files as well - delete if corrupted
        if (attributes.size() == 0L) {
          file.delete()
          continue
        }

        if (!file.inputStream().use { processor(file.fileName.toString(), it, false) }) {
          break
        }
      }
    }

    return true
  }

  override fun delete(fileSpec: String, roamingType: RoamingType): Boolean {
    dir.resolve(fileSpec).delete()
    return true
  }
}
