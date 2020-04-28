package com.intellij.util.indexing.diagnostic.dump.paths.resolvers

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.indexing.diagnostic.dump.paths.PortableFilePath
import java.nio.file.Paths

object ProjectRootPortableFilePathResolver : PortableFilePathResolver {
  override fun findFileByPath(project: Project, portableFilePath: PortableFilePath): VirtualFile? {
    if (portableFilePath is PortableFilePath.ProjectRoot) {
      val projectBasePath = project.basePath ?: return null
      return VfsUtil.findFile(Paths.get(projectBasePath), true)
    }
    return null
  }

}