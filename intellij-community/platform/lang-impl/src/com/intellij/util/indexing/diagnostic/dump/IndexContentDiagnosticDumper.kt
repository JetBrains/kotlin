// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.diagnostic.dump

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.containers.ConcurrentBitSet
import com.intellij.util.indexing.FileBasedIndex
import com.intellij.util.indexing.FileBasedIndexImpl
import com.intellij.util.indexing.IndexingBundle
import com.intellij.util.indexing.diagnostic.dump.paths.IndexedFilePath
import com.intellij.util.indexing.diagnostic.dump.paths.PortableFilePaths

object IndexContentDiagnosticDumper {

  fun getIndexContentDiagnostic(project: Project, indicator: ProgressIndicator): IndexContentDiagnostic {
    val indexedFilePaths = arrayListOf<IndexedFilePath>()
    val providers = (FileBasedIndex.getInstance() as FileBasedIndexImpl).getOrderedIndexableFilesProviders(project)
    val visitedFiles = ConcurrentBitSet()

    indicator.text = IndexingBundle.message("index.content.diagnostic.dumping")
    indicator.isIndeterminate = false
    indicator.fraction = 0.0

    val providerNameToOriginalFileIds = hashMapOf<String, MutableSet<Int>>()
    for ((index, provider) in providers.withIndex()) {
      indicator.text2 = provider.debugName
      val providerFileIds = hashSetOf<Int>()
      providerNameToOriginalFileIds[provider.debugName] = providerFileIds
      provider.iterateFiles(project, { fileOrDir ->
        val indexedFilePath = createIndexedFilePath(fileOrDir, project) ?: return@iterateFiles true
        indexedFilePaths += indexedFilePath
        providerFileIds += indexedFilePath.originalFileSystemId
        true
      }, visitedFiles)
      indicator.fraction = (index + 1).toDouble() / providers.size
    }
    return IndexContentDiagnostic(indexedFilePaths, providerNameToOriginalFileIds)
  }

  private fun createIndexedFilePath(fileOrDir: VirtualFile, project: Project): IndexedFilePath? {
    if (!PortableFilePaths.isSupportedFileSystem(fileOrDir)) {
      // TODO: consider not excluding any file systems.
      return null
    }
    val fileId = FileBasedIndex.getFileId(fileOrDir)
    val fileUrl = fileOrDir.url
    val fileType = fileOrDir.fileType.name
    val portableFilePath = PortableFilePaths.getPortableFilePath(fileOrDir, project)
    val resolvedFile = PortableFilePaths.findFileByPath(portableFilePath, project)
    val indexedFilePath = IndexedFilePath(fileId, fileType, fileUrl, portableFilePath)
    check(fileUrl == resolvedFile?.url) {
      buildString {
        appendln("File cannot be resolved")
        appendln(indexedFilePath.toString())
      }
    }
    return indexedFilePath
  }

}