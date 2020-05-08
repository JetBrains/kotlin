// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.roots

import com.intellij.openapi.module.Module
import com.intellij.testFramework.ApplicationRule
import com.intellij.testFramework.rules.ProjectModelRule
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test

class ConcurrentRootModelModificationTest {
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
  fun `commit one model and dispose another`() {
    val foo = projectModel.addProjectLevelLibrary("foo")
    val bar = projectModel.addProjectLevelLibrary("bar")
    val model1 = createModifiableModel(module)
    val model2 = createModifiableModel(module)
    model1.addLibraryEntry(foo)
    model2.addLibraryEntry(bar)
    commitModifiableRootModel(model1)
    model2.dispose()
    val libraryEntry = dropModuleSourceEntry(ModuleRootManager.getInstance(module), 1).single() as LibraryOrderEntry
    assertThat(libraryEntry.library).isEqualTo(foo)
  }

  @Test
  fun `last committed model wins`() {
    val foo = projectModel.addProjectLevelLibrary("foo")
    val bar = projectModel.addProjectLevelLibrary("bar")
    val model1 = createModifiableModel(module)
    model1.addLibraryEntry(foo)
    val model2 = createModifiableModel(module)
    model2.addLibraryEntry(bar)
    commitModifiableRootModel(model1)
    commitModifiableRootModel(model2)
    val libraryEntry = dropModuleSourceEntry(ModuleRootManager.getInstance(module), 1).single() as LibraryOrderEntry
    assertThat(libraryEntry.library).isEqualTo(bar)
  }
}