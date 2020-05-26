// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.roots

import com.intellij.openapi.module.Module
import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.ApplicationRule
import com.intellij.testFramework.DisposableRule
import com.intellij.testFramework.rules.ProjectModelRule
import com.intellij.testFramework.rules.TempDirectory
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.jps.model.module.UnknownSourceRootType
import org.jetbrains.jps.model.serialization.JpsModelSerializerExtension
import org.junit.Before
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test

class CustomSourceRootTypeTest {
  companion object {
    @JvmField
    @ClassRule
    val appRule = ApplicationRule()
  }

  @Rule
  @JvmField
  val projectModel = ProjectModelRule()

  @Rule
  @JvmField
  val tempDirectory = TempDirectory()

  @Rule
  @JvmField
  val disposable = DisposableRule()

  lateinit var module: Module

  @Before
  fun setUp() {
    module = projectModel.createModule()
    JpsModelSerializerExtension.getExtensions()
  }

  @Test
  fun `load unload custom source root`() {
    val srcDir = projectModel.baseProjectDir.newVirtualDirectory("src")
    runWithRegisteredExtension {
      val model = createModifiableModel(module)
      model.addContentEntry(srcDir).addSourceFolder(srcDir, TestCustomSourceRootType.INSTANCE, TestCustomSourceRootProperties("hello"))
      val committed = commitModifiableRootModel(model)
      val sourceFolder = committed.contentEntries.single().sourceFolders.single()
      assertThat(sourceFolder.file).isEqualTo(srcDir)
      assertThat(sourceFolder.rootType).isEqualTo(TestCustomSourceRootType.INSTANCE)
      assertThat((sourceFolder.jpsElement.properties as TestCustomSourceRootProperties).testString).isEqualTo("hello")
    }

    val sourceFolderWithUnknownType = ModuleRootManager.getInstance(module).contentEntries.single().sourceFolders.single()
    assertThat(sourceFolderWithUnknownType.file).isEqualTo(srcDir)
    assertThat(sourceFolderWithUnknownType.rootType).isInstanceOf(UnknownSourceRootType::class.java)

    TestCustomRootModelSerializerExtension.registerTestCustomSourceRootType(tempDirectory.newDirectory(), disposable.disposable)
    val sourceFolder = ModuleRootManager.getInstance(module).contentEntries.single().sourceFolders.single()
    assertThat(sourceFolder.file).isEqualTo(srcDir)
    assertThat(sourceFolder.rootType).isEqualTo(TestCustomSourceRootType.INSTANCE)
    assertThat((sourceFolder.jpsElement.properties as TestCustomSourceRootProperties).testString).isEqualTo("hello")
  }

  private fun runWithRegisteredExtension(action: () -> Unit) {
    val disposable = Disposer.newDisposable()
    TestCustomRootModelSerializerExtension.registerTestCustomSourceRootType(tempDirectory.newDirectory(), disposable)
    try {
      action()
    }
    finally {
      Disposer.dispose(disposable)
    }
  }

}