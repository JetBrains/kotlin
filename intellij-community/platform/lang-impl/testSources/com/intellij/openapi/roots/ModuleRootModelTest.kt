// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.roots

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.application.runWriteActionAndWait
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.impl.RootConfigurationAccessor
import com.intellij.testFramework.ApplicationRule
import com.intellij.testFramework.rules.ProjectModelRule
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.annotations.NotNull
import org.jetbrains.jps.model.java.JavaSourceRootType
import org.junit.Before
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test

class ModuleRootModelTest {
  companion object {
    @JvmField
    @ClassRule
    val appRule = ApplicationRule()
  }

  @Rule
  @JvmField
  val projectModel = ProjectModelRule()

  lateinit var module: Module

  @Before
  fun setUp() {
    module = projectModel.createModule()
  }

  @Test
  fun `empty instance`() {
    val rootModel = ModuleRootManager.getInstance(module)
    assertThat(rootModel.contentEntries).isEmpty()
    assertThat(rootModel.module).isSameAs(module)
    assertThat(rootModel.sdk).isNull()
    assertThat(rootModel.isSdkInherited).isFalse()
    assertThat(rootModel.orderEntries).hasOnlyOneElementSatisfying { assertThat(it).isInstanceOf(ModuleSourceOrderEntry::class.java)}

    val model = createModifiableModel()
    val committed = commit(model)
    assertThat(committed.contentEntries).isEmpty()
    assertThat(committed.module).isSameAs(module)
    assertThat(committed.sdk).isNull()
  }

  @Test
  fun `add edit remove content folder`() {
    val contentRoot = projectModel.baseProjectDir.newVirtualDirectory("content")
    run {
      val model = createModifiableModel()
      val contentEntry = model.addContentEntry(contentRoot)
      assertThat(contentEntry.file).isEqualTo(contentRoot)
      assertThat(contentEntry.url).isEqualTo(contentRoot.url)
      assertThat(contentEntry.sourceFolders).isEmpty()
      assertThat(contentEntry.excludeFolders).isEmpty()
      assertThat(contentEntry.excludePatterns).isEmpty()
      assertThat(contentEntry.isSynthetic).isFalse()
      assertThat(model.contentEntries.single()).isEqualTo(contentEntry)
      assertThat(contentEntry.rootModel.contentRoots.single()).isEqualTo(contentRoot)
      checkConsistency(contentEntry)
      val committed = commit(model)
      val committedEntry = committed.contentEntries.single()
      assertThat(committedEntry.file).isEqualTo(contentRoot)
      assertThat(committedEntry.url).isEqualTo(contentRoot.url)
      assertThat(committedEntry.rootModel.contentRoots.single()).isEqualTo(contentRoot)
      checkConsistency(committedEntry)
    }

    run {
      val model = createModifiableModel()
      val entry = model.contentEntries.single()
      entry.addExcludePattern("*.txt")
      checkConsistency(entry)
      assertThat(entry.excludePatterns).containsExactly("*.txt")
      val committed = commit(model)
      val committedEntry = committed.contentEntries.single()
      assertThat(committedEntry.file).isEqualTo(contentRoot)
      assertThat(committedEntry.excludePatterns).containsExactly("*.txt")
    }

    run {
      val model = createModifiableModel()
      model.removeContentEntry(model.contentEntries.single())
      val committed = commit(model)
      assertThat(committed.contentEntries).isEmpty()
    }
  }

  @Test
  fun `add edit remove source folder`() {
    val contentRoot = projectModel.baseProjectDir.newVirtualDirectory("content")
    val srcRoot = projectModel.baseProjectDir.newVirtualDirectory("content/src")
    run {
      val model = createModifiableModel()
      val contentEntry = model.addContentEntry(contentRoot)
      val sourceFolder = contentEntry.addSourceFolder(srcRoot, false)
      assertThat(sourceFolder.file).isEqualTo(srcRoot)
      assertThat(sourceFolder.url).isEqualTo(srcRoot.url)
      assertThat(sourceFolder.rootType).isEqualTo(JavaSourceRootType.SOURCE)
      assertThat(sourceFolder.packagePrefix).isEqualTo("")
      assertThat(sourceFolder.isSynthetic).isFalse()
      assertThat(sourceFolder.isTestSource).isFalse()
      assertThat(sourceFolder.contentEntry).isEqualTo(contentEntry)
      assertThat(contentEntry.sourceFolders.single()).isEqualTo(sourceFolder)
      checkConsistency(contentEntry)

      val committed = commit(model)
      val committedContent = committed.contentEntries.single()
      assertThat(committedContent.file).isEqualTo(contentRoot)
      val committedSource = committedContent.sourceFolders.single()
      assertThat(committedSource.file).isEqualTo(srcRoot)
    }

    run {
      val model = createModifiableModel()
      val sourceFolder = model.contentEntries.single().sourceFolders.single()
      sourceFolder.packagePrefix = "foo"
      val committed = commit(model)
      val committedSource = committed.contentEntries.single().sourceFolders.single()
      assertThat(committedSource.packagePrefix).isEqualTo("foo")
    }

    run {
      val model = createModifiableModel()
      val contentEntry = model.contentEntries.single()
      contentEntry.removeSourceFolder(contentEntry.sourceFolders.single())
      assertThat(contentEntry.sourceFolders).isEmpty()
      val committed = commit(model)
      assertThat(committed.contentEntries.single().sourceFolders).isEmpty()
    }
  }

  @Test
  fun `add test source folder`() {
    val contentRoot = projectModel.baseProjectDir.newVirtualDirectory("content")
    val srcRoot = projectModel.baseProjectDir.newVirtualDirectory("content/src")
    val model = createModifiableModel()
    val contentEntry = model.addContentEntry(contentRoot)
    val sourceFolder = contentEntry.addSourceFolder(srcRoot, true)
    assertThat(sourceFolder.file).isEqualTo(srcRoot)
    assertThat(sourceFolder.rootType).isEqualTo(JavaSourceRootType.TEST_SOURCE)
    assertThat(sourceFolder.isTestSource).isTrue()
    checkConsistency(contentEntry)

    val committed = commit(model)
    val committedContent = committed.contentEntries.single()
    assertThat(committedContent.file).isEqualTo(contentRoot)
    val committedSource = committedContent.sourceFolders.single()
    assertThat(committedSource.file).isEqualTo(srcRoot)
  }

  @Test
  fun `add remove excluded folder`() {
    val contentRoot = projectModel.baseProjectDir.newVirtualDirectory("content")
    val excludedRoot1 = projectModel.baseProjectDir.newVirtualDirectory("content/excluded1")
    val excludedRoot2 = projectModel.baseProjectDir.newVirtualDirectory("content/excluded2")
    run {
      val model = createModifiableModel()
      val contentEntry = model.addContentEntry(contentRoot)
      val excluded1 = contentEntry.addExcludeFolder(excludedRoot1)
      val excluded2 = contentEntry.addExcludeFolder(excludedRoot2.url)
      assertThat(excluded1.file).isEqualTo(excludedRoot1)
      assertThat(excluded1.url).isEqualTo(excludedRoot1.url)
      assertThat(excluded2.file).isEqualTo(excludedRoot2)
      assertThat(excluded2.url).isEqualTo(excludedRoot2.url)
      assertThat(excluded1.contentEntry).isEqualTo(contentEntry)
      assertThat(excluded2.contentEntry).isEqualTo(contentEntry)
      assertThat(contentEntry.excludeFolders).containsExactly(excluded1, excluded2)
      
      val committed = commit(model)
      val committedEntry = committed.contentEntries.single()
      assertThat(committedEntry.excludeFolderFiles).containsExactly(excludedRoot1, excludedRoot2)
      val (committedExcluded1, committedExcluded2) = committedEntry.excludeFolders
      assertThat(committedExcluded1.contentEntry).isEqualTo(committedEntry)
      assertThat(committedExcluded2.contentEntry).isEqualTo(committedEntry)
    }

    run {
      val model = createModifiableModel()
      val contentEntry = model.contentEntries.single()
      val excludeFolder2 = contentEntry.excludeFolders[1]
      contentEntry.removeExcludeFolder(excludedRoot1.url)
      assertThat(contentEntry.excludeFolders).containsExactly(excludeFolder2)
      contentEntry.removeExcludeFolder(excludeFolder2)
      assertThat(contentEntry.excludeFolders).isEmpty()
      val committed = commit(model)
      assertThat(committed.contentEntries.single().excludeFolders).isEmpty()
    }
  }

  @Test
  fun `dispose modifiable model`() {
    val model = createModifiableModel()
    val contentRoot = projectModel.baseProjectDir.newVirtualDirectory("content")
    model.addContentEntry(contentRoot)
    model.dispose()
    assertThat(ModuleRootManager.getInstance(module).contentEntries).isEmpty()
  }

  @Test
  fun `remove content root before commit`() {
    val model = createModifiableModel()
    val contentRoot1 = projectModel.baseProjectDir.newVirtualDirectory("content1")
    val contentRoot2 = projectModel.baseProjectDir.newVirtualDirectory("content2")
    model.addContentEntry(contentRoot1)
    val entry = model.addContentEntry(contentRoot2)
    assertThat(model.contentRoots).containsExactly(contentRoot1, contentRoot2)
    model.removeContentEntry(entry)
    assertThat(model.contentRoots).containsExactly(contentRoot1)
    val committed = commit(model)
    assertThat(committed.contentRoots).containsExactly(contentRoot1)
  }

  @Test
  fun `set module sdk`() {
    val model = createModifiableModel()
    val sdk = projectModel.addSdk(projectModel.createSdk("my sdk"))
    model.sdk = sdk
    assertThat(model.isSdkInherited).isFalse()
    assertThat(model.sdk).isEqualTo(sdk)
    assertThat(model.sdkName).isEqualTo("my sdk")
    val committed = commit(model)
    assertThat(committed.isSdkInherited).isFalse()
    assertThat(committed.sdk).isEqualTo(sdk)
  }

  @Test
  fun `inherit project sdk`() {
    val sdk = projectModel.addSdk(projectModel.createSdk("my sdk"))
    runWriteActionAndWait { projectModel.projectRootManager.projectSdk = sdk }
    val model = createModifiableModel()
    model.inheritSdk()
    assertThat(model.isSdkInherited).isTrue()
    assertThat(model.sdk).isEqualTo(sdk)
    assertThat(model.sdkName).isEqualTo("my sdk")
    val committed = commit(model)
    assertThat(committed.isSdkInherited).isTrue()
    assertThat(committed.sdk).isEqualTo(sdk)
  }

  @Test
  fun `set not yet added sdk as module sdk`() {
    val model = createModifiableModel()
    val sdk = projectModel.createSdk("my sdk")
    model.sdk = sdk
    assertThat(model.isSdkInherited).isFalse()
    assertThat(model.sdk).isEqualTo(sdk)
    assertThat(model.sdkName).isEqualTo("my sdk")
    val committed = commit(model)
    projectModel.addSdk(sdk)
    assertThat(committed.isSdkInherited).isFalse()
    assertThat(committed.sdk).isEqualTo(sdk)
  }

  @Test
  fun `set module sdk by name`() {
    val model = createModifiableModel()
    model.setInvalidSdk("my sdk", projectModel.sdkType.name)
    assertThat(model.isSdkInherited).isFalse()
    assertThat(model.sdk).isNull()
    assertThat(model.sdkName).isEqualTo("my sdk")
    val committed = commit(model)
    assertThat(committed.isSdkInherited).isFalse()
    assertThat(committed.sdk).isNull()
    val sdk = projectModel.addSdk(projectModel.createSdk("my sdk"))
    assertThat(committed.sdk).isEqualTo(sdk)
  }

  @Test
  fun `inherit project sdk by name`() {
    runWriteActionAndWait { projectModel.projectRootManager.setProjectSdkName("my sdk", projectModel.sdkType.name) }
    val model = createModifiableModel()
    model.inheritSdk()
    assertThat(model.isSdkInherited).isTrue()
    assertThat(model.sdk).isNull()
    assertThat(model.sdkName).isEqualTo("my sdk")
    val committed = commit(model)
    assertThat(committed.isSdkInherited).isTrue()
    assertThat(committed.sdk).isNull()
    val sdk = projectModel.addSdk(projectModel.createSdk("my sdk"))
    assertThat(committed.sdk).isEqualTo(sdk)
  }

  @Test
  fun `set module sdk from accessor`() {
    val sdk = projectModel.createSdk("my sdk")
    val model = createModifiableModel(object : RootConfigurationAccessor() {
      override fun getSdk(existing: Sdk?, sdkName: String?): Sdk? {
        return if (sdkName == "my sdk") sdk else existing
      }
    })
    model.setInvalidSdk("my sdk", projectModel.sdkType.name)
    assertThat(model.sdk).isEqualTo(sdk)
    assertThat(model.sdkName).isEqualTo("my sdk")
    val committed = commit(model)
    assertThat(committed.isSdkInherited).isFalse()
    assertThat(committed.sdk).isNull()
    projectModel.addSdk(sdk)
    assertThat(committed.sdk).isEqualTo(sdk)
  }

  @Test
  fun `set project sdk from accessor`() {
    val sdk = projectModel.createSdk("my sdk")
    val model = createModifiableModel(object : RootConfigurationAccessor() {
      override fun getProjectSdk(project: Project?): Sdk = sdk
      override fun getProjectSdkName(project: Project?): String? = "my sdk"
    })
    model.inheritSdk()
    assertThat(model.sdk).isEqualTo(sdk)
    assertThat(model.sdkName).isEqualTo("my sdk")
    val committed = commit(model)
    assertThat(committed.isSdkInherited).isTrue()
    assertThat(committed.sdk).isNull()
    projectModel.addSdk(sdk)
    runWriteActionAndWait { projectModel.projectRootManager.projectSdk = sdk }
    assertThat(committed.sdk).isEqualTo(sdk)
  }

  @Test
  fun `set project sdk from accessor by name`() {
    val sdk = projectModel.createSdk("my sdk")
    val model = createModifiableModel(object : RootConfigurationAccessor() {
      override fun getProjectSdkName(project: Project?): String? = "my sdk"
    })
    model.inheritSdk()
    assertThat(model.sdk).isNull()
    assertThat(model.sdkName).isEqualTo("my sdk")
    val committed = commit(model)
    assertThat(committed.isSdkInherited).isTrue()
    assertThat(committed.sdk).isNull()
    projectModel.addSdk(sdk)
    runWriteActionAndWait { projectModel.projectRootManager.projectSdk = sdk }
    assertThat(committed.sdk).isEqualTo(sdk)
  }

  private fun commit(model: @NotNull ModifiableRootModel): ModuleRootManager {
    checkConsistency(model)
    runWriteActionAndWait { model.commit() }
    val moduleRootManager = ModuleRootManager.getInstance(module)
    checkConsistency(moduleRootManager)
    return moduleRootManager
  }

  private fun createModifiableModel(accessor: RootConfigurationAccessor = RootConfigurationAccessor()): ModifiableRootModel {
    return runReadAction {
      val moduleRootManager = ModuleRootManagerEx.getInstanceEx(module)
      checkConsistency(moduleRootManager)
      val model = moduleRootManager.getModifiableModel(accessor)
      checkConsistency(model)
      model
    }
  }

  private fun checkConsistency(rootModel: ModuleRootModel) {
    assertThat(rootModel.contentRoots).containsExactly(*rootModel.contentEntries.mapNotNull { it.file }.toTypedArray())
    assertThat(rootModel.contentRootUrls).containsExactly(*rootModel.contentEntries.map { it.url }.toTypedArray())
    assertThat(rootModel.excludeRoots).containsExactly(*rootModel.contentEntries.flatMap { it.excludeFolderFiles.asIterable() }.toTypedArray())
    assertThat(rootModel.excludeRootUrls).containsExactly(*rootModel.contentEntries.flatMap { it.excludeFolderUrls.asIterable() }.toTypedArray())

    val allSourceRoots = rootModel.contentEntries.flatMap { it.sourceFolderFiles.asIterable() }
    assertThat(rootModel.sourceRoots).containsExactly(*allSourceRoots.toTypedArray())
    assertThat(rootModel.getSourceRoots(true)).containsExactly(*allSourceRoots.toTypedArray())
    assertThat(rootModel.getSourceRoots(false)).containsExactly(*rootModel.contentEntries.flatMap { contentEntry ->
      contentEntry.sourceFolders.filter { !it.isTestSource }.mapNotNull { it.file }.asIterable()
    }.toTypedArray())

    val allSourceRootUrls = rootModel.contentEntries.flatMap { contentEntry -> contentEntry.sourceFolders.map { it.url }.asIterable() }
    assertThat(rootModel.sourceRootUrls).containsExactly(*allSourceRootUrls.toTypedArray())
    assertThat(rootModel.getSourceRootUrls(true)).containsExactly(*allSourceRootUrls.toTypedArray())
    assertThat(rootModel.getSourceRootUrls(false)).containsExactly(*rootModel.contentEntries.flatMap { contentEntry ->
      contentEntry.sourceFolders.filter { !it.isTestSource }.map { it.url }.asIterable()
    }.toTypedArray())

    assertThat(rootModel.dependencyModuleNames).containsExactly(*rootModel.orderEntries.filterIsInstance(ModuleOrderEntry::class.java).map {
      it.moduleName
    }.toTypedArray())
    val moduleDependencies = rootModel.orderEntries.filterIsInstance(ModuleOrderEntry::class.java).mapNotNull { it.module }.toTypedArray()
    assertThat(rootModel.moduleDependencies).containsExactly(*moduleDependencies)
    assertThat(rootModel.getModuleDependencies(true)).containsExactly(*moduleDependencies)
    assertThat(rootModel.getModuleDependencies(false)).containsExactly(
      *rootModel.orderEntries.filterIsInstance(ModuleOrderEntry::class.java).filter { it.scope != DependencyScope.TEST }.mapNotNull { it.module }.toTypedArray()
    )
  }

  private fun checkConsistency(entry: ContentEntry) {
    assertThat(entry.sourceFolderFiles).containsExactly(*entry.sourceFolders.mapNotNull { it.file }.toTypedArray())
    assertThat(entry.excludeFolderFiles).containsExactly(*entry.excludeFolders.mapNotNull { it.file }.toTypedArray())
    assertThat(entry.excludeFolderUrls).containsExactly(*entry.excludeFolders.map { it.url }.toTypedArray())
  }
}