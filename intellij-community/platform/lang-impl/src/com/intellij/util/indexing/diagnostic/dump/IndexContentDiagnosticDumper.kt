// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.diagnostic.dump

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.impl.FilePropertyPusher
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.containers.ConcurrentBitSet
import com.intellij.util.indexing.FileBasedIndex
import com.intellij.util.indexing.FileBasedIndexImpl
import com.intellij.util.indexing.IndexingBundle
import com.intellij.util.indexing.diagnostic.dump.paths.IndexedFilePath
import com.intellij.util.indexing.diagnostic.dump.paths.PortableFilePaths

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

    for ((index, provider) in providers.withIndex()) {
      indicator.text2 = provider.debugName
      val providerFileIds = hashSetOf<Int>()
      providerNameToOriginalFileIds[provider.debugName] = providerFileIds
      provider.iterateFiles(project, { fileOrDir ->
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
      filesFromUnsupportedFileSystem,
      providerNameToOriginalFileIds
    )
  }

  fun createIndexedFilePath(fileOrDir: VirtualFile, project: Project): IndexedFilePath {
    val fileId = FileBasedIndex.getFileId(fileOrDir)
    val fileUrl = fileOrDir.url
    val fileType = fileOrDir.fileType.name
    val fileSize = if (fileOrDir.isDirectory) 0 else fileOrDir.length
    val portableFilePath = PortableFilePaths.getPortableFilePath(fileOrDir, project)
    val resolvedFile = PortableFilePaths.findFileByPath(portableFilePath, project)
    val allPusherValues = dumpFilePropertyPusherValues(fileOrDir, project).mapValues { it.value?.toString() ?: "<null-value>" }
    val indexedFilePath = IndexedFilePath(fileId, fileType, fileSize, fileUrl, portableFilePath, allPusherValues)
    check(fileUrl == resolvedFile?.url) {
      buildString {
        appendln("File cannot be resolved")
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