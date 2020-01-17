// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.service.project

import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.externalSystem.test.AbstractExternalSystemTest
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ModuleRootModel
import com.intellij.openapi.roots.SourceFolder
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.util.PathUtil
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable

abstract class ExternalProjectServiceTestCase extends AbstractExternalSystemTest {

  protected void createProjectSubDirectory(@NotNull String relativePath) {
    def file = new File(projectDir, relativePath)
    FileUtil.ensureExists(file)
    def fileSystem = LocalFileSystem.getInstance()
    fileSystem.refreshAndFindFileByIoFile(file)
  }

  protected void assertSourcePackagePrefix(@NotNull String moduleName, @NotNull String sourcePath, @NotNull String packagePrefix) {
    def module = ReadAction.compute { ModuleManager.getInstance(project).findModuleByName(moduleName) }
    assertNotNull("Module $moduleName not found", module)
    assertSourcePackagePrefix(module, sourcePath, packagePrefix)
  }

  protected static void assertSourcePackagePrefix(@NotNull Module module, @NotNull String sourcePath, @NotNull String packagePrefix) {
    def rootManger = ModuleRootManager.getInstance(module)
    def sourceFolder = findSourceFolder(rootManger, sourcePath)
    assertNotNull("Source folder $sourcePath not found in module ${module.name}", sourceFolder)
    assertEquals(packagePrefix, sourceFolder.getPackagePrefix())
  }

  @Nullable
  private static SourceFolder findSourceFolder(@NotNull ModuleRootModel moduleRootManager, @NotNull String sourcePath) {
    def contentEntries = moduleRootManager.getContentEntries()
    def module = moduleRootManager.getModule()
    def rootUrl = getAbsolutePath(ExternalSystemApiUtil.getExternalProjectPath(module))
    for (def contentEntry : contentEntries) {
      for (def sourceFolder : contentEntry.getSourceFolders()) {
        def folderPath = getAbsolutePath(sourceFolder.getUrl())
        def rootPath = getAbsolutePath("$rootUrl/$sourcePath")
        if (folderPath == rootPath) return sourceFolder
      }
    }
    return null
  }

  @NotNull
  private static String getAbsolutePath(@NotNull String url) {
    def path = VfsUtilCore.urlToPath(url)
    path = PathUtil.getCanonicalPath(path)
    return FileUtil.toSystemIndependentName(path)
  }
}
