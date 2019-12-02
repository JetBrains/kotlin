// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
@file:JvmName("CrcUtils")

package com.intellij.openapi.externalSystem.util

import com.intellij.lang.LanguageParserDefinitions
import com.intellij.lang.ParserDefinition
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.impl.LoadTextUtil
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.UserDataHolder
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import java.util.zip.CRC32

fun Document.calculateCrc(project: Project, file: VirtualFile): Long {
  file.getCachedCrc(modificationStamp)?.let { return it }
  return findOrCalculateCrc(modificationStamp) {
    doCalculateCrc(project, file.fileType)
  }
}

fun VirtualFile.calculateCrc(project: Project) =
  findOrCalculateCrc(modificationStamp) {
    doCalculateCrc(project)
  }

private fun <T : UserDataHolder> T.findOrCalculateCrc(modificationStamp: Long, calculate: () -> Long?): Long {
  ApplicationManager.getApplication().assertReadAccessAllowed()
  val cachedCrc = getCachedCrc(modificationStamp)
  if (cachedCrc != null) return cachedCrc
  val crc = calculate() ?: modificationStamp
  setCachedCrc(crc, modificationStamp)
  return crc
}

private fun doCalculateCrc(project: Project, charSequence: CharSequence, fileType: FileType): Long? {
  val parserDefinition = getParserDefinition(fileType) ?: return null
  val lexer = parserDefinition.createLexer(project)
  val whiteSpaceTokens = parserDefinition.whitespaceTokens
  val commentTokens = parserDefinition.commentTokens
  val ignoredTokens = TokenSet.orSet(commentTokens, whiteSpaceTokens)
  val crc32 = CRC32()
  lexer.start(charSequence)
  ProgressManager.checkCanceled()
  while (true) {
    val tokenType = lexer.tokenType ?: break
    val tokenText = charSequence.subSequence(lexer.tokenStart, lexer.tokenEnd)
    crc32.update(tokenType, tokenText, ignoredTokens)
    lexer.advance()
    ProgressManager.checkCanceled()
  }
  return crc32.value
}

private fun CRC32.update(tokenType: IElementType, tokenText: CharSequence, ignoredTokens: TokenSet) {
  if (ignoredTokens.contains(tokenType)) return
  if (tokenText.isBlank()) return
  update(tokenText)
}

private fun CRC32.update(charSequence: CharSequence) {
  update(charSequence.length)
  for (ch in charSequence) {
    update(ch.toInt())
  }
}

private fun getParserDefinition(fileType: FileType): ParserDefinition? {
  return when (fileType) {
    is LanguageFileType -> LanguageParserDefinitions.INSTANCE.forLanguage(fileType.language)
    else -> null
  }
}

private fun Document.doCalculateCrc(project: Project, fileType: FileType) =
  when {
    fileType.isBinary -> null
    else -> doCalculateCrc(project, immutableCharSequence, fileType)
  }

private fun VirtualFile.doCalculateCrc(project: Project) =
  when {
    isDirectory -> null
    fileType.isBinary -> null
    else -> doCalculateCrc(project, LoadTextUtil.loadText(this), fileType)
  }

private fun UserDataHolder.getCachedCrc(modificationStamp: Long): Long? {
  val (value, stamp) = getUserData(CRC_CACHE) ?: return null
  if (stamp == modificationStamp) return value
  return null
}

private fun UserDataHolder.setCachedCrc(value: Long, modificationStamp: Long) {
  putUserData(CRC_CACHE, CrcCache(value, modificationStamp))
}

private val CRC_CACHE = Key<CrcCache>("com.intellij.openapi.externalSystem.util.CRC_CACHE")

private data class CrcCache(val value: Long, val modificationStamp: Long)
