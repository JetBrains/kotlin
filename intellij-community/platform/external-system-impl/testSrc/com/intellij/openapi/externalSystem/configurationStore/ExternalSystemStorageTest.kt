// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.configurationStore

import com.intellij.facet.FacetManager
import com.intellij.facet.FacetType
import com.intellij.facet.mock.MockFacetType
import com.intellij.facet.mock.MockSubFacetType
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.AppUIExecutor
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.application.appSystemDir
import com.intellij.openapi.application.ex.PathManagerEx
import com.intellij.openapi.application.impl.coroutineDispatchingContext
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.externalSystem.ExternalSystemModulePropertyManager
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.externalSystem.model.project.ModuleData
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsDataStorage
import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsManagerImpl
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.module.ModuleTypeId
import com.intellij.openapi.project.ExternalStorageConfigurationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.doNotEnableExternalStorageByDefaultInTests
import com.intellij.openapi.project.getProjectCacheFileName
import com.intellij.openapi.roots.ExternalProjectSystemRegistry
import com.intellij.openapi.roots.LanguageLevelProjectExtension
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.packaging.artifacts.ArtifactManager
import com.intellij.packaging.elements.ArtifactRootElement
import com.intellij.packaging.elements.PackagingElementFactory
import com.intellij.packaging.impl.artifacts.PlainArtifactType
import com.intellij.packaging.impl.elements.ArchivePackagingElement
import com.intellij.pom.java.LanguageLevel
import com.intellij.project.stateStore
import com.intellij.testFramework.*
import com.intellij.util.io.*
import com.intellij.util.ui.UIUtil
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.annotations.NotNull
import org.junit.*
import org.junit.Assert.assertFalse
import java.nio.file.Files
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
  val disposableRule = DisposableRule()

  @JvmField
  @Rule
  val tempDirManager = TemporaryDirectory()

  @Test
  fun `save single mavenized module`() = saveProjectInExternalStorageAndCheckResult("singleModule") { project, projectDir ->
    val module = ModuleManager.getInstance(project).newModule(projectDir.resolve("test.iml").systemIndependentPath, ModuleTypeId.JAVA_MODULE)
    ModuleRootModificationUtil.addContentRoot(module, projectDir.systemIndependentPath)
    ExternalSystemModulePropertyManager.getInstance(module).setMavenized(true)
  }

  @Test
  fun `load single mavenized module`() = loadProjectAndCheckResults("singleModule") { project ->
    val module = ModuleManager.getInstance(project).modules.single()
    assertThat(module.name).isEqualTo("test")
    assertThat(module.moduleTypeName).isEqualTo(ModuleTypeId.JAVA_MODULE)
    assertThat(module.moduleFilePath).isEqualTo("${project.basePath}/test.iml")
    assertThat(ExternalSystemModulePropertyManager.getInstance(module).isMavenized()).isTrue()
    assertThat(ExternalStorageConfigurationManager.getInstance(project).isEnabled).isTrue()
  }

  @Test
  fun `save single module from external system`() = saveProjectInExternalStorageAndCheckResult("singleModuleFromExternalSystem") { project, projectDir ->
    val module = ModuleManager.getInstance(project).newModule(projectDir.resolve("test.iml").systemIndependentPath,
                                                              ModuleTypeId.JAVA_MODULE)
    ModuleRootModificationUtil.addContentRoot(module, projectDir.systemIndependentPath)
    setExternalSystemOptions(module, projectDir)
  }

  @Test
  fun `load single module from external system`() = loadProjectAndCheckResults("singleModuleFromExternalSystem") { project ->
    val module = ModuleManager.getInstance(project).modules.single()
    assertThat(module.name).isEqualTo("test")
    assertThat(module.moduleTypeName).isEqualTo(ModuleTypeId.JAVA_MODULE)
    assertThat(module.moduleFilePath).isEqualTo("${project.basePath}/test.iml")
    assertThat(ExternalSystemModulePropertyManager.getInstance(module).isMavenized()).isFalse()
    assertThat(ExternalStorageConfigurationManager.getInstance(project).isEnabled).isTrue()
    checkExternalSystemOptions(module, project.basePath!!)
  }

  @Test
  fun `save single module from external system in internal storage`() = saveProjectInInternalStorageAndCheckResult("singleModuleFromExternalSystemInInternalStorage") { project, projectDir ->
    val module = ModuleManager.getInstance(project).newModule(projectDir.resolve("test.iml").systemIndependentPath,
                                                              ModuleTypeId.JAVA_MODULE)
    ModuleRootModificationUtil.addContentRoot(module, projectDir.systemIndependentPath)
    setExternalSystemOptions(module, projectDir)
  }

  @Test
  fun `load single module from external system in internal storage`() = loadProjectAndCheckResults("singleModuleFromExternalSystemInInternalStorage") { project ->
    val module = ModuleManager.getInstance(project).modules.single()
    assertThat(module.name).isEqualTo("test")
    assertThat(module.moduleTypeName).isEqualTo(ModuleTypeId.JAVA_MODULE)
    assertThat(module.moduleFilePath).isEqualTo("${project.basePath}/test.iml")
    assertThat(ExternalSystemModulePropertyManager.getInstance(module).isMavenized()).isFalse()
    assertThat(ExternalStorageConfigurationManager.getInstance(project).isEnabled).isFalse()
    checkExternalSystemOptions(module, project.basePath!!)
  }

  private fun setExternalSystemOptions(module: Module, projectDir: Path) {
    val propertyManager = ExternalSystemModulePropertyManager.getInstance(module)
    val systemId = ProjectSystemId("GRADLE")
    val moduleData = ModuleData("test", systemId, "", "", "", projectDir.systemIndependentPath).also {
      it.group = "group"
      it.version = "42.0"
    }
    val projectData = ProjectData(systemId, "", "", projectDir.systemIndependentPath)
    propertyManager.setExternalOptions(systemId, moduleData, projectData)
  }

  private fun checkExternalSystemOptions(module: Module, projectDirPath: String) {
    val propertyManager = ExternalSystemModulePropertyManager.getInstance(module)
    assertThat(propertyManager.getExternalSystemId()).isEqualTo("GRADLE")
    assertThat(propertyManager.getExternalModuleGroup()).isEqualTo("group")
    assertThat(propertyManager.getExternalModuleVersion()).isEqualTo("42.0")
    assertThat(propertyManager.getLinkedProjectId()).isEqualTo("test")
    assertThat(propertyManager.getLinkedProjectPath()).isEqualTo(projectDirPath)
    assertThat(propertyManager.getRootProjectPath()).isEqualTo(projectDirPath)
  }


  @Test
  fun `save imported module in internal storage`() = saveProjectInInternalStorageAndCheckResult("singleModuleInInternalStorage") { project, projectDir ->
    val module = ModuleManager.getInstance(project).newModule(projectDir.resolve("test.iml").systemIndependentPath,
                                                              ModuleTypeId.JAVA_MODULE)
    ModuleRootModificationUtil.addContentRoot(module, projectDir.systemIndependentPath)
    ExternalSystemModulePropertyManager.getInstance(module).setMavenized(true)
  }

  @Test
  fun `load imported module from internal storage`() = loadProjectAndCheckResults("singleModuleInInternalStorage") { project ->
    val module = ModuleManager.getInstance(project).modules.single()
    assertThat(module.name).isEqualTo("test")
    assertThat(module.moduleTypeName).isEqualTo(ModuleTypeId.JAVA_MODULE)
    assertThat(module.moduleFilePath).isEqualTo("${project.basePath}/test.iml")
    assertThat(ExternalSystemModulePropertyManager.getInstance(module).isMavenized()).isTrue()
    assertThat(ExternalStorageConfigurationManager.getInstance(project).isEnabled).isFalse()
  }

  @Test
  fun `save mixed modules`() = saveProjectInExternalStorageAndCheckResult("mixedModules") { project, projectDir ->
    val regular = ModuleManager.getInstance(project).newModule(projectDir.resolve("regular.iml").systemIndependentPath, ModuleTypeId.JAVA_MODULE)
    ModuleRootModificationUtil.addContentRoot(regular, projectDir.resolve("regular").systemIndependentPath)
    val imported = ModuleManager.getInstance(project).newModule(projectDir.resolve("imported.iml").systemIndependentPath, ModuleTypeId.JAVA_MODULE)
    ModuleRootModificationUtil.addContentRoot(imported, projectDir.resolve("imported").systemIndependentPath)
    ExternalSystemModulePropertyManager.getInstance(imported).setMavenized(true)
    ExternalSystemModulePropertyManager.getInstance(imported).setLinkedProjectPath("${project.basePath}/imported")
  }

  @Test
  fun `load mixed modules`() = loadProjectAndCheckResults("mixedModules") { project ->
    val modules = ModuleManager.getInstance(project).modules.sortedBy { it.name }
    assertThat(modules).hasSize(2)
    val (imported, regular) = modules
    assertThat(imported.name).isEqualTo("imported")
    assertThat(regular.name).isEqualTo("regular")
    assertThat(imported.moduleTypeName).isEqualTo(ModuleTypeId.JAVA_MODULE)
    assertThat(regular.moduleTypeName).isEqualTo(ModuleTypeId.JAVA_MODULE)
    assertThat(imported.moduleFilePath).isEqualTo("${project.basePath}/imported.iml")
    assertThat(regular.moduleFilePath).isEqualTo("${project.basePath}/regular.iml")
    assertThat(ModuleRootManager.getInstance(imported).contentRootUrls.single()).isEqualTo(VfsUtil.pathToUrl("${project.basePath}/imported"))
    assertThat(ModuleRootManager.getInstance(regular).contentRootUrls.single()).isEqualTo(VfsUtil.pathToUrl("${project.basePath}/regular"))
    val externalModuleProperty = ExternalSystemModulePropertyManager.getInstance(imported)
    assertThat(externalModuleProperty.isMavenized()).isTrue()
    assertThat(externalModuleProperty.getLinkedProjectPath()).isEqualTo("${project.basePath}/imported")
    assertThat(ExternalSystemModulePropertyManager.getInstance(regular).isMavenized()).isFalse()
  }

  @Test
  fun `save regular facet in imported module`() = saveProjectInExternalStorageAndCheckResult("regularFacetInImportedModule") { project, projectDir ->
    val imported = ModuleManager.getInstance(project).newModule(projectDir.resolve("imported.iml").systemIndependentPath, ModuleTypeId.JAVA_MODULE)
    FacetManager.getInstance(imported).addFacet(MockFacetType.getInstance(), "regular", null)
    ExternalSystemModulePropertyManager.getInstance(imported).setMavenized(true)
  }

  @Test
  fun `load regular facet in imported module`() = loadProjectAndCheckResults("regularFacetInImportedModule") { project ->
    val module = ModuleManager.getInstance(project).modules.single()
    assertThat(module.name).isEqualTo("imported")
    assertThat(ExternalSystemModulePropertyManager.getInstance(module).isMavenized()).isTrue()
    val facet = FacetManager.getInstance(module).allFacets.single()
    assertThat(facet.name).isEqualTo("regular")
    assertThat(facet.type).isEqualTo(MockFacetType.getInstance())
    assertThat(facet.externalSource).isNull()
  }

  @Test
  fun `do not load modules from external system dir if external storage is disabled`() =
    loadProjectAndCheckResults("externalStorageIsDisabled") { project ->
      assertThat(ModuleManager.getInstance(project).modules).isEmpty()
    }

  @Test
  fun `save imported facet in imported module`() = saveProjectInExternalStorageAndCheckResult("importedFacetInImportedModule") { project, projectDir ->
    val imported = ModuleManager.getInstance(project).newModule(projectDir.resolve("imported.iml").systemIndependentPath, ModuleTypeId.JAVA_MODULE)
    val facetManager = FacetManager.getInstance(imported)
    val model = facetManager.createModifiableModel()
    val source = ExternalProjectSystemRegistry.getInstance().getSourceById(ExternalProjectSystemRegistry.MAVEN_EXTERNAL_SOURCE_ID)
    model.addFacet(facetManager.createFacet(MockFacetType.getInstance(), "imported", null), source)
    model.commit()
    ExternalSystemModulePropertyManager.getInstance(imported).setMavenized(true)
  }

  @Test
  fun `load imported facet in imported module`() = loadProjectAndCheckResults("importedFacetInImportedModule") { project ->
    val module = ModuleManager.getInstance(project).modules.single()
    assertThat(ExternalSystemModulePropertyManager.getInstance(module).isMavenized()).isTrue()
    val facet = FacetManager.getInstance(module).allFacets.single()
    assertThat(facet.name).isEqualTo("imported")
    assertThat(facet.externalSource!!.id).isEqualTo(ExternalProjectSystemRegistry.MAVEN_EXTERNAL_SOURCE_ID)
  }

  @Test
  fun `save libraries`() = saveProjectInExternalStorageAndCheckResult("libraries") { project, _ ->
    val libraryTable = LibraryTablesRegistrar.getInstance().getLibraryTable(project)
    val model = libraryTable.modifiableModel
    model.createLibrary("regular", null)
    model.createLibrary("imported", null, externalSource)
    model.commit()
  }

  @Test
  fun `save libraries in internal storage`() = saveProjectInInternalStorageAndCheckResult("librariesInInternalStorage") { project, _ ->
    val libraryTable = LibraryTablesRegistrar.getInstance().getLibraryTable(project)
    val model = libraryTable.modifiableModel
    model.createLibrary("regular", null)
    model.createLibrary("imported", null, externalSource)
    model.commit()
  }

  @Test
  fun `load libraries`() = loadProjectAndCheckResults("libraries") { project ->
    val libraryTable = LibraryTablesRegistrar.getInstance().getLibraryTable(project)
    runInEdtAndWait {
      UIUtil.dispatchAllInvocationEvents()
    }
    val libraries = libraryTable.libraries.sortedBy { it.name }
    assertThat(libraries).hasSize(2)
    val (imported, regular) = libraries
    assertThat(imported.name).isEqualTo("imported")
    assertThat(regular.name).isEqualTo("regular")
    assertThat(imported.externalSource!!.id).isEqualTo("test")
    assertThat(regular.externalSource).isNull()
  }

  @Test
  fun `save artifacts`() = saveProjectInExternalStorageAndCheckResult("artifacts") { project, projectDir ->
    val model = ArtifactManager.getInstance(project).createModifiableModel()
    val regular = model.addArtifact("regular", PlainArtifactType.getInstance())
    regular.outputPath = projectDir.resolve("out/artifacts/regular").systemIndependentPath
    val root = PackagingElementFactory.getInstance().createArchive("a.jar")
    val imported = model.addArtifact("imported", PlainArtifactType.getInstance(), root, externalSource)
    imported.outputPath = projectDir.resolve("out/artifacts/imported").systemIndependentPath
    model.commit()
  }

  @Test
  fun `load artifacts`() = loadProjectAndCheckResults("artifacts") { project ->
    val artifacts = ArtifactManager.getInstance(project).sortedArtifacts
    assertThat(artifacts).hasSize(2)
    val (imported, regular) = artifacts
    assertThat(imported.name).isEqualTo("imported")
    assertThat(regular.name).isEqualTo("regular")
    assertThat(imported.externalSource!!.id).isEqualTo("test")
    assertThat(regular.externalSource).isNull()
    assertThat(imported.outputPath).isEqualTo("${project.basePath}/out/artifacts/imported")
    assertThat(regular.outputPath).isEqualTo("${project.basePath}/out/artifacts/regular")
    assertThat((imported.rootElement as ArchivePackagingElement).name).isEqualTo("a.jar")
    assertThat(regular.rootElement).isInstanceOf(ArtifactRootElement::class.java)
  }

  @Before
  fun registerFacetType() {
    WriteAction.runAndWait<RuntimeException> {
      FacetType.EP_NAME.getPoint().registerExtension(MockFacetType(), disposableRule.disposable)
      FacetType.EP_NAME.getPoint().registerExtension(MockSubFacetType(), disposableRule.disposable)
    }
  }

  private val externalSource get() = ExternalProjectSystemRegistry.getInstance().getSourceById("test")

  private fun saveProjectInInternalStorageAndCheckResult(testDataDirName: String, setupProject: (Project, Path) -> Unit) {
    doNotEnableExternalStorageByDefaultInTests {
      saveProjectAndCheckResult(testDataDirName, false, setupProject)
    }
  }

  private fun saveProjectInExternalStorageAndCheckResult(testDataDirName: String, setupProject: (Project, Path) -> Unit) {
    saveProjectAndCheckResult(testDataDirName, true, setupProject)
  }

  private fun saveProjectAndCheckResult(testDataDirName: String,
                                        storeExternally: Boolean,
                                        setupProject: (Project, Path) -> Unit) {
    runBlocking {
      createProjectAndUseInLoadComponentStateMode(tempDirManager, directoryBased = true, useDefaultProjectSettings = false) { project ->
        ExternalProjectsManagerImpl.getInstance(project).setStoreExternally(storeExternally)
        val projectDir = Paths.get(project.stateStore.directoryStorePath).parent
        val cacheDir = ExternalProjectsDataStorage.getProjectConfigurationDir(project)
        cacheDir.delete()
        Disposer.register(disposableRule.disposable, Disposable { cacheDir.delete() })

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
        FileUtil.copyDir(testDataRoot.resolve("common").toFile(), expectedDir.toFile())
        FileUtil.copyDir(testDataRoot.resolve(testDataDirName).toFile(), expectedDir.toFile())

        projectDir.toFile().assertMatches(directoryContentOf(expectedDir.resolve("project")))
        val expectedCacheDir = expectedDir.resolve("cache")
        if (Files.exists(expectedCacheDir)) {
          cacheDir.toFile().assertMatches(directoryContentOf(expectedCacheDir), FileTextMatcher.ignoreBlankLines())
        }
        else {
          assertFalse("$cacheDir doesn't exist", Files.exists(cacheDir))
        }
      }
    }
  }

  private val testDataRoot
    get() = Paths.get(PathManagerEx.getCommunityHomePath()).resolve("platform/external-system-impl/testData/jpsSerialization")

  private fun loadProjectAndCheckResults(testDataDirName: String, checkProject: (Project) -> Unit) {
    @Suppress("RedundantSuspendModifier")
    suspend fun copyProjectFiles(dir: VirtualFile): Path {
      val projectDir = VfsUtil.virtualToIoFile(dir)
      FileUtil.copyDir(testDataRoot.resolve("common/project").toFile(), projectDir)
      val testProjectFilesDir = testDataRoot.resolve(testDataDirName).resolve("project").toFile()
      if (testProjectFilesDir.exists()) {
        FileUtil.copyDir(testProjectFilesDir, projectDir)
      }
      val testCacheFilesDir = testDataRoot.resolve(testDataDirName).resolve("cache").toFile()
      if (testCacheFilesDir.exists()) {
        val cachePath = appSystemDir.resolve("external_build_system").resolve(getProjectCacheFileName(projectDir.absolutePath))
        FileUtil.copyDir(testCacheFilesDir, cachePath.toFile())
      }
      VfsUtil.markDirtyAndRefresh(false, true, true, dir)
      return projectDir.toPath()
    }
    doNotEnableExternalStorageByDefaultInTests {
      runBlocking {
        createOrLoadProject(tempDirManager, ::copyProjectFiles, loadComponentState = true, useDefaultProjectSettings = false) {
          checkProject(it)
        }
      }
    }
  }
}