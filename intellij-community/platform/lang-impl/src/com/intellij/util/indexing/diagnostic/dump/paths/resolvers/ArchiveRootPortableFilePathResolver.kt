package com.intellij.util.indexing.diagnostic.dump.paths.resolvers

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.impl.VirtualFileManagerImpl
import com.intellij.openapi.vfs.newvfs.ArchiveFileSystem
import com.intellij.util.indexing.diagnostic.dump.paths.PortableFilePath
import com.intellij.util.indexing.diagnostic.dump.paths.PortableFilePaths

object ArchiveRootPortableFilePathResolver : PortableFilePathResolver {

  private val archiveFileSystems by lazy {
    (VirtualFileManager.getInstance() as VirtualFileManagerImpl)
      .physicalFileSystems
      .filterIsInstance<ArchiveFileSystem>()
  }

  override fun findFileByPath(project: Project, portableFilePath: PortableFilePath): VirtualFile? {
    if (portableFilePath is PortableFilePath.ArchiveRoot) {
      val archiveLocalFile = PortableFilePaths.findFileByPath(portableFilePath.archiveLocalPath, project) ?: return null
      return archiveFileSystems.asSequence()
               .mapNotNull { it.getRootByLocal(archiveLocalFile) }
               .firstOrNull() ?: return null
    }
    return null
  }
}