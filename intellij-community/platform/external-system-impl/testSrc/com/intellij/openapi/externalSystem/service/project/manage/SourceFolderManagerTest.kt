// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.service.project.manage

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.module.ModuleTypeId
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.testFramework.HeavyPlatformTestCase
import com.intellij.testFramework.PlatformTestUtil
import org.assertj.core.api.BDDAssertions.then
import org.jetbrains.jps.model.java.JavaSourceRootType
import java.io.File
import java.util.concurrent.Future

class SourceFolderManagerTest: HeavyPlatformTestCase() {

  fun `test source folder is added to content root when created`() {
    val rootManager = ModuleRootManager.getInstance(module)
    val dir = createTempDir("contentEntry")
    val modifiableModel = rootManager.modifiableModel
    modifiableModel.addContentEntry(VfsUtilCore.pathToUrl(dir.absolutePath))
    runWriteAction {
      modifiableModel.commit()
    }

    val manager: SourceFolderManagerImpl = SourceFolderManager.getInstance(project) as SourceFolderManagerImpl

    val folderUrl = ModuleRootManager.getInstance(module).contentRootUrls[0] + "/newFolder";
    val folderFile = File(VfsUtilCore.urlToPath(folderUrl))

    manager.addSourceFolder(module, folderUrl, JavaSourceRootType.SOURCE)

    val file = File(folderFile, "file.txt")
    FileUtil.writeToFile(file, "SomeContent");

    LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file)
    val bulkOperationState: Future<*>? = manager.bulkOperationState
    if (bulkOperationState == null) {
      fail("Source Folder manager operation expected")
    } else {
      PlatformTestUtil.waitForFuture(bulkOperationState, 1000)
    }

    then(rootManager.contentEntries[0].sourceFolders)
      .hasSize(1)
      .extracting("url")
      .containsExactly(folderUrl)
  }

  fun `test new content root is created if source folder does not belong to existing one`() {
    val rootManager = ModuleRootManager.getInstance(module)
    val dir = createTempDir("contentEntry")
    createModuleWithContentRoot(dir)

    val manager:SourceFolderManagerImpl = SourceFolderManager.getInstance(project) as SourceFolderManagerImpl
    val folderFile = File(dir, "newFolder")
    val folderUrl = VfsUtilCore.pathToUrl(folderFile.absolutePath)

    manager.addSourceFolder(module, folderUrl, JavaSourceRootType.SOURCE)

    val file = File(folderFile, "file.txt")
    FileUtil.writeToFile(file, "SomeContent");

    LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file)
    val bulkOperationState: Future<*>? = manager.bulkOperationState
    if (bulkOperationState == null) {
      fail("Source Folder manager operation expected")
    } else {
      PlatformTestUtil.waitForFuture(bulkOperationState, 1000)
    }

    then(rootManager
           .contentEntries
           .flatMap { it.sourceFolders.asList() }
           .map { it.url })
      .containsExactly(folderUrl)
  }

  private fun createModuleWithContentRoot(dir: File): Module {
    val moduleManager = ModuleManager.getInstance(project)
    val modifiableModel = moduleManager.modifiableModel
    val newModule: Module =
      try {
        modifiableModel.newModule(File(dir, "topModule").absolutePath, ModuleTypeId.JAVA_MODULE)
    } finally {
        runWriteAction {
          modifiableModel.commit()
        }
    }

    val modifiableRootModel = ModuleRootManager.getInstance(newModule).modifiableModel
    try {
      modifiableRootModel.addContentEntry(VfsUtilCore.pathToUrl(dir.absolutePath))
    } finally {
      runWriteAction {
        modifiableRootModel.commit()
      }
    }

    return newModule
  }
}