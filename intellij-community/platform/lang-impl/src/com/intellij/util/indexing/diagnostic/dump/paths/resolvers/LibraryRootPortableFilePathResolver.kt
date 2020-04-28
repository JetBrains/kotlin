package com.intellij.util.indexing.diagnostic.dump.paths.resolvers

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.indexing.diagnostic.dump.paths.PortableFilePath

object LibraryRootPortableFilePathResolver : PortableFilePathResolver {
  override fun findFileByPath(project: Project, portableFilePath: PortableFilePath): VirtualFile? {
    if (portableFilePath is PortableFilePath.LibraryRoot) {
      val libraryTable = LibraryTablesRegistrar.getInstance().getLibraryTable(project)
      val library = libraryTable.getLibraryByName(portableFilePath.libraryName) ?: return null
      val rootType = if (portableFilePath.inClassFiles) OrderRootType.CLASSES else OrderRootType.SOURCES
      val roots = library.rootProvider.getFiles(rootType)
      return roots.getOrNull(portableFilePath.libraryRootIndex)
    }
    return null
  }
}