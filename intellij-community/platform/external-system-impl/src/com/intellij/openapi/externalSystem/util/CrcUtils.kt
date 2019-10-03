// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
@file:JvmName("CrcUtils")

package com.intellij.openapi.externalSystem.util

import com.intellij.lang.cacheBuilder.CacheBuilderRegistry
import com.intellij.lang.cacheBuilder.WordOccurrence
import com.intellij.lang.cacheBuilder.WordsScanner
import com.intellij.lang.findUsages.LanguageFindUsages
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.impl.LoadTextUtil
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import java.util.zip.CRC32

fun VirtualFile.calculateCrc(): Long {
  ApplicationManager.getApplication().assertReadAccessAllowed()
  val cachedCrc = getCachedCrc(modificationStamp)
  if (cachedCrc != null) return cachedCrc
  val crc = doCalculateCrc()
  ProgressManager.checkCanceled()
  setCachedCrc(crc, modificationStamp)
  return crc
}

private fun VirtualFile.doCalculateCrc(): Long {
  if (isDirectory) {
    LOG.debug("Cannot calculate CRC for directory '$path'")
    return modificationStamp
  }

  val wordsScanner = getScanner(this)
  if (wordsScanner == null) {
    LOG.debug("WordsScanner not found for file '$path'")
    return modificationStamp
  }

  val crc32 = CRC32()
  wordsScanner.processWords(LoadTextUtil.loadText(this)) {
    if (it.kind !== WordOccurrence.Kind.COMMENTS) {
      for (ch in it.baseText.subSequence(it.start, it.end)) {
        crc32.update(ch.toInt())
      }
    }
    true
  }
  return crc32.value
}

private fun getScanner(file: VirtualFile): WordsScanner? {
  val fileType = file.fileType
  val customWordsScanner = CacheBuilderRegistry.getInstance().getCacheBuilder(fileType)
  if (customWordsScanner != null) {
    return customWordsScanner
  }

  if (fileType is LanguageFileType) {
    val lang = fileType.language
    return LanguageFindUsages.getWordsScanner(lang)
  }
  return null
}

private fun VirtualFile.getCachedCrc(modificationStamp: Long): Long? {
  val (value, stamp) = getUserData(CRC_CACHE) ?: return null
  if (stamp == modificationStamp) return value
  return null
}

private fun VirtualFile.setCachedCrc(value: Long, modificationStamp: Long) {
  putUserData(CRC_CACHE, CrcCache(value, modificationStamp))
}

private val LOG = Logger.getInstance("#com.intellij.openapi.externalSystem.autoimport")

private val CRC_CACHE = Key<CrcCache>("com.intellij.openapi.externalSystem.util.CRC_CACHE")

private data class CrcCache(val value: Long, val modificationStamp: Long)
