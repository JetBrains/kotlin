package com.intellij.util.indexing.diagnostic.dump.paths.resolvers

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.util.indexing.diagnostic.dump.paths.PortableFilePath

object AbsolutePortableFilePathResolver : PortableFilePathResolver {
  override fun findFileByPath(project: Project, portableFilePath: PortableFilePath): VirtualFile? {
    if (portableFilePath is PortableFilePath.AbsolutePath) {
      return VirtualFileManager.getInstance().refreshAndFindFileByUrl(portableFilePath.absoluteUrl)
    }
    return null
  }
}