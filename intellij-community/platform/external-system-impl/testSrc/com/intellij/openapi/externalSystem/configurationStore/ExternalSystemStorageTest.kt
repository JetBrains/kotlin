// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.configurationStore

import com.intellij.facet.FacetManager
import com.intellij.facet.impl.invalid.InvalidFacetType
import com.intellij.openapi.application.AppUIExecutor
import com.intellij.openapi.application.ex.PathManagerEx
import com.intellij.openapi.application.impl.coroutineDispatchingContext
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.externalSystem.ExternalSystemModulePropertyManager
import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsDataStorage
import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsManagerImpl
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.module.ModuleTypeId
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.CompilerProjectExtension
import com.intellij.openapi.roots.ExternalProjectSystemRegistry
import com.intellij.openapi.roots.LanguageLevelProjectExtension
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.packaging.artifacts.ArtifactManager
import com.intellij.packaging.elements.PackagingElementFactory
import com.intellij.packaging.impl.artifacts.PlainArtifactType
import com.intellij.pom.java.LanguageLevel
import com.intellij.project.stateStore
import com.intellij.testFramework.*
import com.intellij.util.io.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import java.nio.file.Path
import java.nio.file.Paths

@RunsInActiveStoreMode
class ExternalSystemStorageTest {
  companion object {
    @JvmField
    @ClassRule
    val appRule = ApplicationRule()
  }

  @JvmField
  @Rule
  val tempDirManager = TemporaryDirectory()

  @Test
  fun `single module`() = saveProjectAndCheckResult("singleModule") { project, projectDir ->
    val module = ModuleManager.getInstance(project).newModule(projectDir.resolve("test.iml").systemIndependentPath, ModuleTypeId.JAVA_MODULE)
    ModuleRootModificationUtil.addContentRoot(module, projectDir.systemIndependentPath)
    ExternalSystemModulePropertyManager.getInstance(module).setMavenized(true)
  }

  @Test
  fun `mixed modules`() = saveProjectAndCheckResult("mixedModules") { project, projectDir ->
    val regular = ModuleManager.getInstance(project).newModule(projectDir.resolve("regular.iml").systemIndependentPath, ModuleTypeId.JAVA_MODULE)
    ModuleRootModificationUtil.addContentRoot(regular, projectDir.resolve("regular").systemIndependentPath)
    val imported = ModuleManager.getInstance(project).newModule(projectDir.resolve("imported.iml").systemIndependentPath, ModuleTypeId.JAVA_MODULE)
    ModuleRootModificationUtil.addContentRoot(imported, projectDir.resolve("imported").systemIndependentPath)
    ExternalSystemModulePropertyManager.getInstance(imported).setMavenized(true)
  }

  @Test
  fun libraries() = saveProjectAndCheckResult("libraries") { project, projectDir ->
    val libraryTable = LibraryTablesRegistrar.getInstance().getLibraryTable(project)
    val model = libraryTable.modifiableModel
    model.createLibrary("regular", null)
    model.createLibrary("imported", null, externalSource)
    model.commit()
  }

  @Test
  fun artifacts() = saveProjectAndCheckResult("artifacts") { project, projectDir ->
    val model = ArtifactManager.getInstance(project).createModifiableModel()
    val regular = model.addArtifact("regular", PlainArtifactType.getInstance())
    regular.outputPath = projectDir.resolve("out/artifacts/regular").systemIndependentPath
    val root = PackagingElementFactory.getInstance().createArchive("a.jar")
    val imported = model.addArtifact("imported", PlainArtifactType.getInstance(), root, externalSource)
    imported.outputPath = projectDir.resolve("out/artifacts/imported").systemIndependentPath
    model.commit()
  }

  private val externalSource get() = ExternalProjectSystemRegistry.getInstance().getSourceById("test")

  private fun saveProjectAndCheckResult(testDataDirName: String, setupProject: (Project, Path) -> Unit) {
    runBlocking {
      createProjectAndUseInLoadComponentStateMode(tempDirManager, directoryBased = true) { project ->
        ExternalProjectsManagerImpl.getInstance(project).setStoreExternally(true)
        val projectDir = Paths.get(project.stateStore.directoryStorePath).parent
        val cacheDir = ExternalProjectsDataStorage.getProjectConfigurationDir(project)
        cacheDir.delete()

        withContext(AppUIExecutor.onUiThread().coroutineDispatchingContext()) {
          runWriteAction {
            //we need to set language level explicitly because otherwise if some tests modifies language level in the default project, we'll
            // get different content in misc.xml
            LanguageLevelProjectExtension.getInstance(project)!!.languageLevel = LanguageLevel.JDK_1_8
            setupProject(project, projectDir)
          }
        }

        project.stateStore.save()

        val expectedDir = tempDirManager.newPath("expectedStorage")
        val testDataRoot = Paths.get(PathManagerEx.getCommunityHomePath()).resolve("platform/external-system-impl/testData/jpsSerialization")
        FileUtil.copyDir(testDataRoot.resolve("common").toFile(), expectedDir.toFile())
        FileUtil.copyDir(testDataRoot.resolve(testDataDirName).toFile(), expectedDir.toFile())

        projectDir.toFile().assertMatches(directoryContentOf(expectedDir.resolve("project")))
        cacheDir.toFile().assertMatches(directoryContentOf(expectedDir.resolve("cache")))
      }
    }
  }
}