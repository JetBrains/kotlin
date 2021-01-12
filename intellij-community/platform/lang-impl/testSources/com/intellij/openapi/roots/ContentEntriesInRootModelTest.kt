// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.roots

import com.intellij.openapi.module.Module
import com.intellij.testFramework.ApplicationRule
import com.intellij.testFramework.rules.ProjectModelRule
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.jps.model.java.JavaSourceRootType
import org.junit.Before
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test

class ContentEntriesInRootModelTest {
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
    val moduleSourceEntry = rootModel.orderEntries.single() as ModuleSourceOrderEntry
    assertThat(moduleSourceEntry.ownerModule).isSameAs(module)

    val model = createModifiableModel(module)
    val entryInModel = model.orderEntries.single() as ModuleSourceOrderEntry
    assertThat(entryInModel.ownerModule).isSameAs(module)
    assertThat(entryInModel.rootModel).isSameAs(model)

    val committed = commitModifiableRootModel(model, assertChanged = false)
    assertThat(committed.contentEntries).isEmpty()
    assertThat(committed.module).isSameAs(module)
    assertThat(committed.sdk).isNull()
  }

  @Test
  fun `add edit remove content folder`() {
    val contentRoot = projectModel.baseProjectDir.newVirtualDirectory("content")
    run {
      val model = createModifiableModel(module)
      val contentEntry = model.addContentEntry(contentRoot)
      assertThat(contentEntry.file).isEqualTo(contentRoot)
      assertThat(contentEntry.url).isEqualTo(contentRoot.url)
      assertThat(contentEntry.sourceFolders).isEmpty()
      assertThat(contentEntry.excludeFolders).isEmpty()
      assertThat(contentEntry.excludePatterns).isEmpty()
      assertThat(contentEntry.isSynthetic).isFalse()
      assertThat(model.contentEntries.single()).isEqualTo(contentEntry)
      assertThat(contentEntry.rootModel.contentRoots.single()).isEqualTo(contentRoot)
      checkContentEntryConsistency(contentEntry)
      val committed = commitModifiableRootModel(model)
      val committedEntry = committed.contentEntries.single()
      assertThat(committedEntry.file).isEqualTo(contentRoot)
      assertThat(committedEntry.url).isEqualTo(contentRoot.url)
      assertThat(committedEntry.rootModel.contentRoots.single()).isEqualTo(contentRoot)
      checkContentEntryConsistency(committedEntry)
    }

    run {
      val model = createModifiableModel(module)
      val entry = model.contentEntries.single()
      entry.addExcludePattern("*.txt")
      checkContentEntryConsistency(entry)
      assertThat(entry.excludePatterns).containsExactly("*.txt")
      val committed = commitModifiableRootModel(model)
      val committedEntry = committed.contentEntries.single()
      assertThat(committedEntry.file).isEqualTo(contentRoot)
      assertThat(committedEntry.excludePatterns).containsExactly("*.txt")
    }

    run {
      val model = createModifiableModel(module)
      model.removeContentEntry(model.contentEntries.single())
      val committed = commitModifiableRootModel(model)
      assertThat(committed.contentEntries).isEmpty()
    }
  }

  @Test
  fun `add edit remove source folder`() {
    val contentRoot = projectModel.baseProjectDir.newVirtualDirectory("content")
    val srcRoot = projectModel.baseProjectDir.newVirtualDirectory("content/src")
    run {
      val model = createModifiableModel(module)
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
      checkContentEntryConsistency(contentEntry)

      val committed = commitModifiableRootModel(model)
      val committedContent = committed.contentEntries.single()
      assertThat(committedContent.file).isEqualTo(contentRoot)
      val committedSource = committedContent.sourceFolders.single()
      assertThat(committedSource.file).isEqualTo(srcRoot)
    }

    run {
      val model = createModifiableModel(module)
      val sourceFolder = model.contentEntries.single().sourceFolders.single()
      sourceFolder.packagePrefix = "foo"
      val committed = commitModifiableRootModel(model)
      val committedSource = committed.contentEntries.single().sourceFolders.single()
      assertThat(committedSource.packagePrefix).isEqualTo("foo")
    }

    run {
      val model = createModifiableModel(module)
      val contentEntry = model.contentEntries.single()
      contentEntry.removeSourceFolder(contentEntry.sourceFolders.single())
      assertThat(contentEntry.sourceFolders).isEmpty()
      val committed = commitModifiableRootModel(model)
      assertThat(committed.contentEntries.single().sourceFolders).isEmpty()
    }
  }

  @Test
  fun `add test source folder`() {
    val contentRoot = projectModel.baseProjectDir.newVirtualDirectory("content")
    val srcRoot = projectModel.baseProjectDir.newVirtualDirectory("content/src")
    val model = createModifiableModel(module)
    val contentEntry = model.addContentEntry(contentRoot)
    val sourceFolder = contentEntry.addSourceFolder(srcRoot, true)
    assertThat(sourceFolder.file).isEqualTo(srcRoot)
    assertThat(sourceFolder.rootType).isEqualTo(JavaSourceRootType.TEST_SOURCE)
    assertThat(sourceFolder.isTestSource).isTrue()
    checkContentEntryConsistency(contentEntry)

    val committed = commitModifiableRootModel(model)
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
      val model = createModifiableModel(module)
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

      val committed = commitModifiableRootModel(model)
      val committedEntry = committed.contentEntries.single()
      assertThat(committedEntry.excludeFolderFiles).containsExactly(excludedRoot1, excludedRoot2)
      val (committedExcluded1, committedExcluded2) = committedEntry.excludeFolders
      assertThat(committedExcluded1.contentEntry).isEqualTo(committedEntry)
      assertThat(committedExcluded2.contentEntry).isEqualTo(committedEntry)
    }

    run {
      val model = createModifiableModel(module)
      val contentEntry = model.contentEntries.single()
      val excludeFolder2 = contentEntry.excludeFolders[1]
      contentEntry.removeExcludeFolder(excludedRoot1.url)
      assertThat(contentEntry.excludeFolders).containsExactly(excludeFolder2)
      contentEntry.removeExcludeFolder(excludeFolder2)
      assertThat(contentEntry.excludeFolders).isEmpty()
      val committed = commitModifiableRootModel(model)
      assertThat(committed.contentEntries.single().excludeFolders).isEmpty()
    }
  }

  @Test
  fun `dispose modifiable model`() {
    val model = createModifiableModel(module)
    val contentRoot = projectModel.baseProjectDir.newVirtualDirectory("content")
    model.addContentEntry(contentRoot)
    model.dispose()
    assertThat(ModuleRootManager.getInstance(module).contentEntries).isEmpty()
  }

  @Test
  fun `remove content root before commit`() {
    val model = createModifiableModel(module)
    val contentRoot1 = projectModel.baseProjectDir.newVirtualDirectory("content1")
    val contentRoot2 = projectModel.baseProjectDir.newVirtualDirectory("content2")
    model.addContentEntry(contentRoot1)
    val entry = model.addContentEntry(contentRoot2)
    assertThat(model.contentRoots).containsExactly(contentRoot1, contentRoot2)
    model.removeContentEntry(entry)
    assertThat(model.contentRoots).containsExactly(contentRoot1)
    val committed = commitModifiableRootModel(model)
    assertThat(committed.contentRoots).containsExactly(contentRoot1)
  }

  @Test
  fun `content entries are sorted`() {
    val model = createModifiableModel(module)
    val contentRoot1 = projectModel.baseProjectDir.newVirtualDirectory("content1")
    val contentRoot2 = projectModel.baseProjectDir.newVirtualDirectory("content2")
    model.addContentEntry(contentRoot2)
    assertThat(model.contentRoots).containsExactly(contentRoot2)
    model.addContentEntry(contentRoot1)
    assertThat(model.contentRoots).containsExactly(contentRoot1, contentRoot2)
    val committed = commitModifiableRootModel(model)
    assertThat(committed.contentRoots).containsExactly(contentRoot1, contentRoot2)
  }

  @Test
  fun clear() {
    val contentRoot = projectModel.baseProjectDir.newVirtualDirectory("content1")
    ModuleRootModificationUtil.addContentRoot(module, contentRoot)
    val model = createModifiableModel(module)
    model.clear()
    assertThat(model.contentEntries).isEmpty()
    val committed = commitModifiableRootModel(model)
    assertThat(committed.contentEntries).isEmpty()
    val moduleSourceEntry = committed.orderEntries.single() as ModuleSourceOrderEntry
    assertThat(moduleSourceEntry.ownerModule).isSameAs(module)
  }
}