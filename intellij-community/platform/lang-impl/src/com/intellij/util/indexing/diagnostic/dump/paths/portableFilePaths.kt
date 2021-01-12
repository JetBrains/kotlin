// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.diagnostic.dump.paths

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.ArchiveFileSystem
import com.intellij.util.indexing.diagnostic.dump.paths.providers.*
import com.intellij.util.indexing.diagnostic.dump.paths.resolvers.*
import java.io.PrintWriter

object PortableFilePaths {

  private val PROVIDERS: List<PortableFilePathProvider> = listOf(
    JdkPortableFilePathProvider,
    LibraryRelativePortableFilePathProvider,
    IdePortableFilePathProvider,
    ProjectRelativePortableFilePathProvider
  )

  private val RESOLVERS: List<PortableFilePathResolver> = listOf(
    JdkRootPortableFilePathResolver,
    LibraryRootPortableFilePathResolver,
    ArchiveRootPortableFilePathResolver,
    IdeRootPortableFilePathResolver,
    ProjectRootPortableFilePathResolver,
    RelativePortableFilePathResolver
  )

  fun getPortableFilePath(virtualFile: VirtualFile, project: Project): PortableFilePath =
    PROVIDERS.asSequence().mapNotNull { it.getRelativePortableFilePath(project, virtualFile) }.firstOrNull()
    ?: PortableFilePath.AbsolutePath(virtualFile.url)

  fun findFileByPath(portableFilePath: PortableFilePath, project: Project): VirtualFile? =
    RESOLVERS.asSequence().mapNotNull { it.findFileByPath(project, portableFilePath) }.firstOrNull()
    ?: AbsolutePortableFilePathResolver.findFileByPath(project, portableFilePath)

  fun isSupportedFileSystem(virtualFile: VirtualFile): Boolean =
    virtualFile.isInLocalFileSystem || virtualFile.fileSystem is ArchiveFileSystem

}

object PortableFilePathsPersistence {

  private val jacksonMapper = jacksonObjectMapper()

  fun serializeToString(portableFilePath: PortableFilePath): String =
    jacksonMapper.writeValueAsString(portableFilePath)

  fun deserializeFromString(string: String): PortableFilePath =
    jacksonMapper.readValue<PortableFilePath>(string)
}

class PortableFilesDumpCollector(private val project: Project) {
  private val myBrokenFiles = mutableSetOf<PortableFilePath>()
  private val myExistingFiles = mutableSetOf<PortableFilePath>()

  fun addFiles(files: Iterable<VirtualFile>) = apply {
    files.forEach { addFile(it) }
  }

  fun addFile(file: VirtualFile) = apply {
    val portable = PortableFilePaths.getPortableFilePath(file, project)

    val targetSet = when {
      file.exists() && file.isValid -> myExistingFiles
      else -> myBrokenFiles
    }

    targetSet += portable
  }

  private data class SerializableData(
    val broken: List<String>,
    val files: List<String>
  )

  private fun snapshot(files: Collection<PortableFilePath>): List<String> {
    return files
      .map { it.presentablePath }
      .toSortedSet()
      .toList()
  }

  private fun snapshot()  = SerializableData(
    broken = snapshot(myBrokenFiles),
    files = snapshot(myExistingFiles)
  )

  fun writeTo(printWriter: PrintWriter, indent: String) {
    val snapshot = snapshot()
    snapshot.broken.forEach { printWriter.append("${indent}BROKEN: ").append(it).appendln() }
    snapshot.files.forEach { printWriter.append("${indent}$it").appendln() }
  }

  fun serializeToText() : String {
    val snapshot = snapshot()
    return buildString {
      snapshot.broken.forEach { append("BROKEN: ").append(it).appendln() }
      snapshot.files.forEach { append(it).appendln() }
    }
  }
}
