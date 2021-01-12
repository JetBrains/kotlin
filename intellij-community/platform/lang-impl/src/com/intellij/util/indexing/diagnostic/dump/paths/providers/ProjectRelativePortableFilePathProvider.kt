package com.intellij.util.indexing.diagnostic.dump.paths.providers

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.ArchiveFileSystem
import com.intellij.util.indexing.diagnostic.dump.paths.PortableFilePath
import com.intellij.util.indexing.diagnostic.dump.paths.PortableFilePaths

object ProjectRelativePortableFilePathProvider : PortableFilePathProvider {
  override fun getRelativePortableFilePath(project: Project, virtualFile: VirtualFile): PortableFilePath.RelativePath? {
    val projectBasePath = project.basePath ?: return null
    return getLocalOrArchiveRelativeFilePath(virtualFile, project, projectBasePath, PortableFilePath.ProjectRoot)
  }

  fun getLocalOrArchiveRelativeFilePath(
    virtualFile: VirtualFile,
    project: Project,
    rootBasePath: String,
    root: PortableFilePath
  ): PortableFilePath.RelativePath? {
    val fileSystem = virtualFile.fileSystem
    if (fileSystem is ArchiveFileSystem) {
      val archiveLocalFile = fileSystem.getLocalByEntry(virtualFile) ?: return null
      val archiveRootFile = fileSystem.getRootByEntry(virtualFile) ?: return null
      val relativePath = VfsUtil.getRelativePath(virtualFile, archiveRootFile) ?: return null
      val archiveLocalPath: PortableFilePath = PortableFilePaths.getPortableFilePath(archiveLocalFile, project)
      val archiveRoot = PortableFilePath.ArchiveRoot(archiveLocalPath)
      return PortableFilePath.RelativePath(archiveRoot, relativePath)
    }

    // Usual non-archive local pth.
    val filePath = virtualFile.path
    val systemIndependentBase = FileUtil.toSystemIndependentName(rootBasePath)
    if (FileUtil.isAncestor(systemIndependentBase, filePath, false)) {
      val relativePath = FileUtil.getRelativePath(systemIndependentBase, filePath, '/')
      if (relativePath != null) {
        return PortableFilePath.RelativePath(root, relativePath)
      }
    }
    return null
  }
}