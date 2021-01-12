package com.intellij.util.indexing.diagnostic.dump.paths.resolvers

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.util.indexing.diagnostic.dump.paths.PortableFilePath
import com.intellij.util.indexing.diagnostic.dump.paths.PortableFilePaths

object RelativePortableFilePathResolver : PortableFilePathResolver {
  override fun findFileByPath(project: Project, portableFilePath: PortableFilePath): VirtualFile? {
    if (portableFilePath is PortableFilePath.RelativePath) {
      val rootFile = PortableFilePaths.findFileByPath(portableFilePath.root, project) ?: return null
      val fullUrl = rootFile.url.trimEnd('/') + '/' + portableFilePath.relativePath
      return VirtualFileManager.getInstance().refreshAndFindFileByUrl(fullUrl)
    }
    return null
  }

}