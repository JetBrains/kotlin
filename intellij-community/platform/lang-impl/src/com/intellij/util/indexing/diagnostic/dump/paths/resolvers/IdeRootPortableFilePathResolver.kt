package com.intellij.util.indexing.diagnostic.dump.paths.resolvers

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.indexing.diagnostic.dump.paths.PortableFilePath
import com.intellij.util.indexing.diagnostic.dump.paths.providers.IdePortableFilePathProvider
import java.nio.file.Paths

object IdeRootPortableFilePathResolver : PortableFilePathResolver {
  override fun findFileByPath(project: Project, portableFilePath: PortableFilePath): VirtualFile? {
    if (portableFilePath is PortableFilePath.IdeRoot) {
      val ideDirectoryPath = IdePortableFilePathProvider.IDE_PATHS[portableFilePath.ideDirectoryType]
      checkNotNull(ideDirectoryPath) { portableFilePath.ideDirectoryType }
      return VfsUtil.findFile(Paths.get(ideDirectoryPath), true)
    }
    return null
  }
}