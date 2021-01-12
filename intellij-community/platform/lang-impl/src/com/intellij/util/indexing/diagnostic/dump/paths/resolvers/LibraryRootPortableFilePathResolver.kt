package com.intellij.util.indexing.diagnostic.dump.paths.resolvers

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.libraries.LibraryTable
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.indexing.diagnostic.dump.paths.PortableFilePath

object LibraryRootPortableFilePathResolver : PortableFilePathResolver {
  override fun findFileByPath(project: Project, portableFilePath: PortableFilePath): VirtualFile? {
    if (portableFilePath is PortableFilePath.LibraryRoot) {
      return when (portableFilePath.libraryType) {
        PortableFilePath.LibraryRoot.LibraryType.APPLICATION -> {
          findInLibraryTable(LibraryTablesRegistrar.getInstance().libraryTable, portableFilePath)
        }
        PortableFilePath.LibraryRoot.LibraryType.PROJECT -> {
          findInLibraryTable(LibraryTablesRegistrar.getInstance().getLibraryTable(project), portableFilePath)
        }
        PortableFilePath.LibraryRoot.LibraryType.MODULE -> {
          val moduleName = portableFilePath.moduleName!!
          val module = ModuleManager.getInstance(project).findModuleByName(moduleName) ?: return null
          val rootModel = runReadAction { ModuleRootManager.getInstance(module).modifiableModel }
          try {
            findInLibraryTable(rootModel.moduleLibraryTable, portableFilePath)
          }
          finally {
            rootModel.dispose()
          }
        }
      }
    }
    return null
  }

  private fun findInLibraryTable(libraryTable: LibraryTable, portableFilePath: PortableFilePath.LibraryRoot): VirtualFile? {
    val library = libraryTable.getLibraryByName(portableFilePath.libraryName) ?: return null
    val rootType = if (portableFilePath.inClassFiles) OrderRootType.CLASSES else OrderRootType.SOURCES
    val roots = library.rootProvider.getFiles(rootType)
    return roots.getOrNull(portableFilePath.libraryRootIndex)
  }
}