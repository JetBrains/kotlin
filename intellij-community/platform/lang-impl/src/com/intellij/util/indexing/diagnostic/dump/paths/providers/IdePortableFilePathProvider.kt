package com.intellij.util.indexing.diagnostic.dump.paths.providers

import com.intellij.openapi.application.PathManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.indexing.diagnostic.dump.paths.PortableFilePath

object IdePortableFilePathProvider : PortableFilePathProvider {

  val IDE_PATHS: Map<String, String> = mapOf(
    "config" to PathManager.getConfigPath(),
    "system" to PathManager.getSystemPath(),
    "log" to PathManager.getLogPath(),
    "plugins" to PathManager.getPluginsPath(),
    "home" to PathManager.getHomePath()
  ).mapValues { FileUtil.toSystemIndependentName(it.value) }

  override fun getRelativePortableFilePath(project: Project, virtualFile: VirtualFile): PortableFilePath.RelativePath? {
    for ((ideDirType, ideDirPath) in IDE_PATHS) {
      val relativeFilePath = ProjectRelativePortableFilePathProvider.getLocalOrArchiveRelativeFilePath(
        virtualFile,
        project,
        ideDirPath,
        PortableFilePath.IdeRoot(ideDirType)
      )
      if (relativeFilePath != null) {
        return relativeFilePath
      }
    }
    return null
  }
}