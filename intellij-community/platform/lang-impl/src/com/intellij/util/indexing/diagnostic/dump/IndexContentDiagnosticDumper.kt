// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.diagnostic.dump

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.impl.FilePropertyPusher
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.containers.ConcurrentBitSet
import com.intellij.util.indexing.*
import com.intellij.util.indexing.diagnostic.dump.paths.IndexedFilePath
import com.intellij.util.indexing.diagnostic.dump.paths.PortableFilePaths
import kotlin.streams.asSequence

object IndexContentDiagnosticDumper {

  fun getIndexContentDiagnostic(project: Project, indicator: ProgressIndicator): IndexContentDiagnostic {
    val providers = (FileBasedIndex.getInstance() as FileBasedIndexImpl).getOrderedIndexableFilesProviders(project)
    val visitedFiles = ConcurrentBitSet()

    indicator.text = IndexingBundle.message("index.content.diagnostic.dumping")
    indicator.isIndeterminate = false
    indicator.fraction = 0.0

    val indexedFilePaths = arrayListOf<IndexedFilePath>()
    val providerNameToOriginalFileIds = hashMapOf<String, MutableSet<Int>>()
    val filesFromUnsupportedFileSystem = arrayListOf<IndexedFilePath>()
    val fileBasedIndexExtensionIds = FileBasedIndexExtension.EXTENSION_POINT_NAME.extensionList.map { it.name }
    val infrastructureExtensions = FileBasedIndexInfrastructureExtension.EP_NAME.extensions().asSequence()
      .mapNotNull { it.createFileIndexingStatusProcessor(project) }
      .toList()

    val providedIndexIdToIndexedFiles = hashMapOf<String, MutableSet<Int>>()

    for ((index, provider) in providers.withIndex()) {
      indicator.text2 = provider.debugName
      val providerFileIds = hashSetOf<Int>()
      providerNameToOriginalFileIds[provider.debugName] = providerFileIds
      provider.iterateFiles(project, { fileOrDir ->
        val fileId = FileBasedIndex.getFileId(fileOrDir)
        for (indexId in fileBasedIndexExtensionIds) {
          if (infrastructureExtensions.any { it.hasIndexForFile(fileOrDir, fileId, indexId) }) {
            providedIndexIdToIndexedFiles.getOrPut(indexId.name) { hashSetOf() } += fileId
          }
        }

        val indexedFilePath = createIndexedFilePath(fileOrDir, project)
        if (PortableFilePaths.isSupportedFileSystem(fileOrDir)) {
          indexedFilePaths += indexedFilePath
          providerFileIds += indexedFilePath.originalFileSystemId
        }
        else {
          // TODO: consider not excluding any file systems.
          filesFromUnsupportedFileSystem += indexedFilePath
          return@iterateFiles true
        }
        true
      }, visitedFiles)
      indicator.fraction = (index + 1).toDouble() / providers.size
    }
    return IndexContentDiagnostic(
      indexedFilePaths,
      providedIndexIdToIndexedFiles,
      filesFromUnsupportedFileSystem,
      providerNameToOriginalFileIds
    )
  }

  fun doesFileHaveProvidedIndex(file: VirtualFile, indexId: ID<*, *>, project: Project): Boolean {
    val fileId = FileBasedIndex.getFileId(file)
    return FileBasedIndexInfrastructureExtension.EP_NAME.extensions().asSequence()
      .mapNotNull { it.createFileIndexingStatusProcessor(project) }
      .any { it.hasIndexForFile(file, fileId, indexId) }
  }

  fun createIndexedFilePath(fileOrDir: VirtualFile, project: Project): IndexedFilePath {
    val fileId = FileBasedIndex.getFileId(fileOrDir)
    val fileUrl = fileOrDir.url
    val fileType = if (fileOrDir.isDirectory) null else fileOrDir.fileType.name
    val substitutedFileType = if (fileOrDir.isDirectory) {
      null
    }
    else {
      runReadAction {
        SubstitutedFileType.substituteFileType(fileOrDir, fileOrDir.fileType, project).name.takeIf { it != fileType }
      }
    }
    val fileSize = if (fileOrDir.isDirectory) null else fileOrDir.length
    val portableFilePath = PortableFilePaths.getPortableFilePath(fileOrDir, project)
    val resolvedFile = PortableFilePaths.findFileByPath(portableFilePath, project)
    val allPusherValues = dumpFilePropertyPusherValues(fileOrDir, project).mapValues { it.value?.toString() ?: "<null-value>" }
    val indexedFilePath = IndexedFilePath(
      fileId,
      fileType,
      substitutedFileType,
      fileSize,
      fileUrl,
      portableFilePath,
      allPusherValues
    )
    check(fileUrl == resolvedFile?.url) {
      buildString {
        appendln("File cannot be resolved")
        appendln("Original URL: $fileUrl")
        appendln("Resolved URL: ${resolvedFile?.url}")
        appendln(indexedFilePath.toString())
      }
    }
    return indexedFilePath
  }

  fun dumpFilePropertyPusherValues(file: VirtualFile, project: Project): Map<String, Any?> {
    val map = hashMapOf<String, Any?>()
    FilePropertyPusher.EP_NAME.forEachExtensionSafe { pusher ->
      if (file.isDirectory && pusher.acceptsDirectory(file, project)
          || !file.isDirectory && pusher.acceptsFile(file, project)
      ) {
        map[pusher.pusherName] = pusher.getImmediateValue(project, file)
      }
    }
    return map
  }

  private val FilePropertyPusher<*>.pusherName: String
    get() = javaClass.name
      .removePrefix("com.")
      .removePrefix("intellij.")
      .removePrefix("jetbrains.")
      .replace("util.", "")
      .replace("impl.", "")


}