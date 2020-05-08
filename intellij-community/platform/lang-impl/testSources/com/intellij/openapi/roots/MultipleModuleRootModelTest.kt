// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.roots

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.application.runWriteActionAndWait
import com.intellij.openapi.roots.impl.ModifiableModelCommitter
import com.intellij.testFramework.ApplicationRule
import com.intellij.testFramework.rules.ProjectModelRule
import org.assertj.core.api.Assertions.assertThat
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test

class MultipleModuleRootModelTest {
  companion object {
    @JvmField
    @ClassRule
    val appRule = ApplicationRule()
  }

  @Rule
  @JvmField
  val projectModel = ProjectModelRule()

  @Test
  fun `commit root model before committing module`() {
    val library = projectModel.addProjectLevelLibrary("lib")
    val moduleModel = runReadAction { projectModel.moduleManager.modifiableModel }
    val module = projectModel.createModule("a", moduleModel)
    val model = createModifiableModel(module)
    model.addLibraryEntry(library)
    val committed = commitModifiableRootModel(model)
    val libraryEntry = dropModuleSourceEntry(committed, 1).single() as LibraryOrderEntry
    assertThat(libraryEntry.library).isEqualTo(library)
    runWriteActionAndWait { moduleModel.commit() }
    val libraryEntryForCommitted = dropModuleSourceEntry(ModuleRootManager.getInstance(module), 1).single() as LibraryOrderEntry
    assertThat(libraryEntryForCommitted.library).isEqualTo(library)
  }

  @Test
  fun `commit root model and dispose module`() {
    val library = projectModel.addProjectLevelLibrary("lib")
    val moduleModel = runReadAction { projectModel.moduleManager.modifiableModel }
    val module = projectModel.createModule("a", moduleModel)
    val model = createModifiableModel(module)
    model.addLibraryEntry(library)
    commitModifiableRootModel(model)
    runWriteActionAndWait { moduleModel.dispose() }
    assertThat(projectModel.moduleManager.modules).isEmpty()
  }

  @Test
  fun `create two modules with dependency between them`() {
    val moduleManager = projectModel.moduleManager
    val moduleModel = runReadAction { moduleManager.modifiableModel }
    val a = projectModel.createModule("a", moduleModel)
    val b = projectModel.createModule("b", moduleModel)
    val model = createModifiableModel(a)
    model.addModuleOrderEntry(b)
    runWriteActionAndWait { ModifiableModelCommitter.multiCommit(listOf(model), moduleModel) }
    assertThat(moduleManager.findModuleByName("a")).isEqualTo(a)
    assertThat(ModuleRootManager.getInstance(a).dependencies.single()).isEqualTo(b)
  }

  @Test
  fun `create two modules with circular dependency between them`() {
    val moduleManager = projectModel.moduleManager
    val moduleModel = runReadAction { moduleManager.modifiableModel }
    val a = projectModel.createModule("a", moduleModel)
    val b = projectModel.createModule("b", moduleModel)
    val modelA = createModifiableModel(a)
    modelA.addModuleOrderEntry(b)
    val modelB = createModifiableModel(b)
    modelB.addModuleOrderEntry(a)
    runWriteActionAndWait { ModifiableModelCommitter.multiCommit(listOf(modelA, modelB), moduleModel) }
    assertThat(moduleManager.findModuleByName("a")).isEqualTo(a)
    assertThat(moduleManager.findModuleByName("b")).isEqualTo(b)
    assertThat(ModuleRootManager.getInstance(a).dependencies.single()).isEqualTo(b)
    assertThat(ModuleRootManager.getInstance(b).dependencies.single()).isEqualTo(a)
  }
}