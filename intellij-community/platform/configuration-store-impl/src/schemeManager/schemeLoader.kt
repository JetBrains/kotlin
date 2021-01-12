// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.configurationStore.schemeManager

import com.intellij.configurationStore.*
import com.intellij.openapi.diagnostic.debug
import com.intellij.openapi.options.NonLazySchemeProcessor
import com.intellij.openapi.project.ProjectBundle
import com.intellij.openapi.util.JDOMUtil
import com.intellij.openapi.vfs.CharsetToolkit
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.io.createDirectories
import com.intellij.util.io.systemIndependentPath
import org.jdom.Element
import org.xmlpull.mxp1.MXParser
import org.xmlpull.v1.XmlPullParser
import java.io.IOException
import java.io.InputStream
import java.nio.file.Path
import java.security.MessageDigest
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Function

internal class SchemeLoader<T : Any, MUTABLE_SCHEME : T>(private val schemeManager: SchemeManagerImpl<T, MUTABLE_SCHEME>,
                                                         private val oldSchemes: List<T>,
                                                         private val preScheduledFilesToDelete: MutableSet<String>,
                                                         private val isDuringLoad: Boolean) {
  private val filesToDelete: MutableSet<String> = HashSet()

  private val schemes: MutableList<T> = oldSchemes.toMutableList()
  private var newSchemesOffset = schemes.size

  // scheme could be changed - so, hashcode will be changed - we must use identity hashing strategy
  private val schemeToInfo = IdentityHashMap<T, ExternalInfo>()

  private val isApplied = AtomicBoolean()

  private var digest: MessageDigest? = null

  // or from current session, or from current state
  private fun getInfoForExistingScheme(existingScheme: T): ExternalInfo? {
    return schemeToInfo.get(existingScheme) ?: schemeManager.schemeToInfo.get(existingScheme)
  }

  private fun isFromFileWithNewExtension(existingScheme: T, fileNameWithoutExtension: String): Boolean {
    return getInfoForExistingScheme(existingScheme)?.fileNameWithoutExtension == fileNameWithoutExtension
  }

  /**
   * Returns list of new schemes.
   */
  fun apply(): List<T> {
    LOG.assertTrue(isApplied.compareAndSet(false, true))
    if (filesToDelete.isNotEmpty() || preScheduledFilesToDelete.isNotEmpty()) {
      LOG.debug { "Schedule to delete: ${filesToDelete.joinToString()} (and preScheduledFilesToDelete: ${preScheduledFilesToDelete.joinToString()})" }
      schemeManager.filesToDelete.addAll(filesToDelete)
      schemeManager.filesToDelete.addAll(preScheduledFilesToDelete)
    }

    schemeManager.schemeToInfo.putAll(schemeToInfo)

    val result = schemes.subList(newSchemesOffset, schemes.size)
    schemeManager.schemeListManager.replaceSchemeList(oldSchemes, schemes)
    if (!isDuringLoad) {
      for (newScheme in result) {
        @Suppress("UNCHECKED_CAST")
        schemeManager.processor.onSchemeAdded(newScheme as MUTABLE_SCHEME)
      }
    }
    return result
  }

  private fun getDigest(): MessageDigest {
    var result = digest
    if (result == null) {
      result = createDataDigest()
      digest = result
    }
    else {
      result.reset()
    }
    return result
  }

  private fun checkExisting(schemeKey: String, fileName: String, fileNameWithoutExtension: String, extension: String): Boolean {
    val processor = schemeManager.processor
    // schemes load session doesn't care about any scheme that added after session creation,
    // e.g. for now, on apply, simply current manager list replaced atomically to the new one
    // if later it will lead to some issues, this check should be done as merge operation (again, currently on apply old list is replaced and not merged)
    val existingSchemeIndex = schemes.indexOfFirst { processor.getSchemeKey(it) == schemeKey }
    val existingScheme = (if (existingSchemeIndex == -1) null else schemes.get(existingSchemeIndex)) ?: return true
    if (schemeManager.schemeListManager.readOnlyExternalizableSchemes.get(processor.getSchemeKey(existingScheme)) === existingScheme) {
      // so, bundled scheme is shadowed
      schemes.removeAt(existingSchemeIndex)
      if (existingSchemeIndex < newSchemesOffset) {
        newSchemesOffset--
      }
      // not added to filesToDelete because it is only shadowed
      return true
    }

    if (processor.isExternalizable(existingScheme)) {
      val existingInfo = getInfoForExistingScheme(existingScheme)
      // is from file with old extension
      if (existingInfo != null && schemeManager.schemeExtension != existingInfo.fileExtension) {
        schemeToInfo.remove(existingScheme)
        existingInfo.scheduleDelete(filesToDelete, "from file with old extension")

        schemes.removeAt(existingSchemeIndex)
        if (existingSchemeIndex < newSchemesOffset) {
          newSchemesOffset--
        }

        // when existing loaded scheme removed, we need to remove it from schemeManager.schemeToInfo,
        // but SchemeManager will correctly remove info on save, no need to complicate
        return true
      }
    }

    if (schemeManager.schemeExtension != extension && isFromFileWithNewExtension(existingScheme, fileNameWithoutExtension)) {
      // 1.oldExt is loading after 1.newExt - we should delete 1.oldExt
      LOG.debug { "Schedule to delete: $fileName (reason: extension mismatch)" }
      filesToDelete.add(fileName)
    }
    else {
      // We don't load scheme with duplicated name - if we generate unique name for it, it will be saved then with new name.
      // It is not what all can expect. Such situation in most cases indicates error on previous level, so, we just warn about it.
      LOG.warn("Scheme file \"$fileName\" is not loaded because defines duplicated name \"$schemeKey\"")
    }
    return false
  }

  fun loadScheme(fileName: String, input: InputStream?, preloadedBytes: ByteArray?): MUTABLE_SCHEME? {
    val extension = schemeManager.getFileExtension(fileName, isAllowAny = false)
    if (isFileScheduledForDeleteInThisLoadSession(fileName)) {
      LOG.warn("Scheme file \"$fileName\" is not loaded because marked to delete")
      return null
    }

    val processor = schemeManager.processor
    val fileNameWithoutExtension = fileName.substring(0, fileName.length - extension.length)

    fun createInfo(schemeName: String, element: Element?): ExternalInfo {
      val info = ExternalInfo(fileNameWithoutExtension, extension)
      if (element != null) {
        info.digest = element.digest(getDigest())
      }
      info.schemeKey = schemeName
      return info
    }

    var scheme: MUTABLE_SCHEME? = null
    if (processor is LazySchemeProcessor) {
      val bytes = preloadedBytes ?: input!!.readBytes()
      lazyPreloadScheme(bytes, schemeManager.isOldSchemeNaming) { name, parser ->
        val attributeProvider = Function<String, String?> {
          if (parser.eventType == XmlPullParser.START_TAG) {
            parser.getAttributeValue(null, it)
          }
          else {
            null
          }
        }
        val schemeKey = name
                        ?: processor.getSchemeKey(attributeProvider, fileNameWithoutExtension)
                        ?: throw nameIsMissed(bytes)
        if (!checkExisting(schemeKey, fileName, fileNameWithoutExtension, extension)) {
          return null
        }

        val externalInfo = createInfo(schemeKey, null)
        scheme = processor.createScheme(SchemeDataHolderImpl(processor, bytes, externalInfo), schemeKey, attributeProvider)
        schemeToInfo.put(scheme!!, externalInfo)
        retainProbablyScheduledForDeleteFile(fileName)
      }
    }
    else {
      val element = when (preloadedBytes) {
        null -> JDOMUtil.load(CharsetToolkit.inputStreamSkippingBOM(input!!.buffered()))
        else -> JDOMUtil.load(CharsetToolkit.inputStreamSkippingBOM(preloadedBytes.inputStream()))
      }
      scheme = (processor as NonLazySchemeProcessor).readScheme(element, isDuringLoad) ?: return null
      val schemeKey = processor.getSchemeKey(scheme!!)
      if (!checkExisting(schemeKey, fileName, fileNameWithoutExtension, extension)) {
        return null
      }

      schemeToInfo.put(scheme!!, createInfo(schemeKey, element))
      retainProbablyScheduledForDeleteFile(fileName)
    }

    schemes.add(scheme!!)
    return scheme
  }

  private fun isFileScheduledForDeleteInThisLoadSession(fileName: String): Boolean {
    return filesToDelete.contains(fileName)
  }

  private fun retainProbablyScheduledForDeleteFile(fileName: String) {
    filesToDelete.remove(fileName)
    preScheduledFilesToDelete.remove(fileName)
  }

  fun removeUpdatedScheme(changedScheme: MUTABLE_SCHEME) {
    val index = ContainerUtil.indexOfIdentity(schemes, changedScheme)
    if (LOG.assertTrue(index >= 0)) {
      schemes.removeAt(index)
      schemeToInfo.remove(changedScheme)
    }
  }
}

internal inline fun lazyPreloadScheme(bytes: ByteArray, isOldSchemeNaming: Boolean, consumer: (name: String?, parser: XmlPullParser) -> Unit) {
  val parser = MXParser()
  parser.setInput(bytes.inputStream().reader())
  consumer(preload(isOldSchemeNaming, parser), parser)
}

private fun preload(isOldSchemeNaming: Boolean, parser: MXParser): String? {
  var eventType = parser.eventType

  fun findName(): String? {
    eventType = parser.next()
    while (eventType != XmlPullParser.END_DOCUMENT) {
      when (eventType) {
        XmlPullParser.START_TAG -> {
          if (parser.name == "option" && parser.getAttributeValue(null, "name") == "myName") {
            return parser.getAttributeValue(null, "value")
          }
        }
      }

      eventType = parser.next()
    }
    return null
  }

  do {
    when (eventType) {
      XmlPullParser.START_TAG -> {
        if (!isOldSchemeNaming || parser.name != "component") {
          if (parser.name == "profile" || (isOldSchemeNaming && parser.name == "copyright")) {
            return findName()
          }
          else if (parser.name == "inspections") {
            // backward compatibility - we don't write PROFILE_NAME_TAG anymore
            return parser.getAttributeValue(null, "profile_name") ?: findName()
          }
          else if (parser.name == "configuration") {
            // run configuration
            return parser.getAttributeValue(null, "name")
          }
          else {
            return null
          }
        }
      }
    }
    eventType = parser.next()
  }
  while (eventType != XmlPullParser.END_DOCUMENT)
  return null
}

internal class ExternalInfo(var fileNameWithoutExtension: String, var fileExtension: String?) {
  // we keep it to detect rename
  var schemeKey: String? = null

  var digest: ByteArray? = null

  val fileName: String
    get() = "$fileNameWithoutExtension$fileExtension"

  fun setFileNameWithoutExtension(nameWithoutExtension: String, extension: String) {
    fileNameWithoutExtension = nameWithoutExtension
    fileExtension = extension
  }

  fun isDigestEquals(newDigest: ByteArray) = Arrays.equals(digest, newDigest)

  fun scheduleDelete(filesToDelete: MutableSet<String>, reason: String) {
    LOG.debug { "Schedule to delete: $fileName (reason: $reason)" }
    filesToDelete.add(fileName)
  }

  override fun toString() = fileName
}

internal fun VirtualFile.getOrCreateChild(fileName: String, requestor: StorageManagerFileWriteRequestor): VirtualFile {
  return findChild(fileName) ?: runAsWriteActionIfNeeded { createChildData(requestor, fileName) }
}

internal fun createDir(ioDir: Path, requestor: StorageManagerFileWriteRequestor): VirtualFile {
  ioDir.createDirectories()
  val parentFile = ioDir.parent
  val parentVirtualFile = (if (parentFile == null) null else VfsUtil.createDirectoryIfMissing(parentFile.systemIndependentPath))
      ?: throw IOException(ProjectBundle.message("project.configuration.save.file.not.found", parentFile))
  return parentVirtualFile.getOrCreateChild(ioDir.fileName.toString(), requestor)
}