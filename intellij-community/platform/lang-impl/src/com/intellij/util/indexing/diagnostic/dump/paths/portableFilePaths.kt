package com.intellij.util.indexing.diagnostic.dump.paths

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.ArchiveFileSystem
import com.intellij.util.indexing.diagnostic.dump.paths.providers.*
import com.intellij.util.indexing.diagnostic.dump.paths.resolvers.*

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

  fun getPortableFilePath(virtualFile: VirtualFile, project: Project): PortableFilePath = runReadAction {
    PROVIDERS.asSequence().mapNotNull { it.getRelativePortableFilePath(project, virtualFile) }.firstOrNull()
    ?: PortableFilePath.AbsolutePath(virtualFile.url)
  }

  fun findFileByPath(portableFilePath: PortableFilePath, project: Project): VirtualFile? = runReadAction {
    RESOLVERS.asSequence().mapNotNull { it.findFileByPath(project, portableFilePath) }.firstOrNull()
    ?: AbsolutePortableFilePathResolver.findFileByPath(project, portableFilePath)
  }

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