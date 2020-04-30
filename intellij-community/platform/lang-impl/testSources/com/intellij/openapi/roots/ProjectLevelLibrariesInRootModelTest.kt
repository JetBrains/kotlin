// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.roots

import com.intellij.openapi.application.runWriteActionAndWait
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.impl.libraries.LibraryEx
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar
import com.intellij.testFramework.ApplicationRule
import com.intellij.testFramework.assertions.Assertions.assertThat
import com.intellij.testFramework.rules.ProjectModelRule
import org.junit.Before
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test

class ProjectLevelLibrariesInRootModelTest {
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
  fun `add edit remove project library`() {
    val library = projectModel.addProjectLevelLibrary("foo")
    run {
      val model = createModifiableModel(module)
      val libraryEntry = model.addLibraryEntry(library)
      assertThat(libraryEntry.isModuleLevel).isFalse()
      assertThat(libraryEntry.libraryName).isEqualTo("foo")
      assertThat(libraryEntry.library).isEqualTo(library)
      assertThat(libraryEntry.libraryLevel).isEqualTo(LibraryTablesRegistrar.PROJECT_LEVEL)
      assertThat(model.findLibraryOrderEntry(library)).isEqualTo(libraryEntry)
      assertThat((library as LibraryEx).isDisposed).isFalse()
      val committed = commitModifiableRootModel(model)
      val committedEntry = dropModuleSourceEntry(committed, 1).single() as LibraryOrderEntry
      assertThat(committedEntry.scope).isEqualTo(DependencyScope.COMPILE)
      assertThat(committedEntry.isExported).isFalse()
      assertThat(committedEntry.library).isEqualTo(library)
    }

    run {
      val model = createModifiableModel(module)
      val libraryEntry = model.findLibraryOrderEntry(library)!!
      libraryEntry.scope = DependencyScope.RUNTIME
      libraryEntry.isExported = true
      val committed = commitModifiableRootModel(model)
      val committedEntry = dropModuleSourceEntry(committed, 1).single() as LibraryOrderEntry
      assertThat(committedEntry.scope).isEqualTo(DependencyScope.RUNTIME)
      assertThat(committedEntry.isExported).isTrue()
    }

    run {
      val model = createModifiableModel(module)
      val libraryEntry = model.findLibraryOrderEntry(library)!!
      model.removeOrderEntry(libraryEntry)
      assertThat(model.orderEntries).hasSize(1)
      val committed = commitModifiableRootModel(model)
      assertThat(committed.orderEntries).hasSize(1)
      assertThat((library as LibraryEx).isDisposed).isFalse()
    }
  }

  @Test
  fun `add same library twice`() {
    val library = projectModel.addProjectLevelLibrary("foo")
    run {
      val model = createModifiableModel(module)
      val libraryEntry1 = model.addLibraryEntry(library)
      val libraryEntry2 = model.addLibraryEntry(library)
      assertThat(libraryEntry1.library).isEqualTo(library)
      assertThat(libraryEntry2.library).isEqualTo(library)
      assertThat(model.findLibraryOrderEntry(library)).isEqualTo(libraryEntry1)
      val committed = commitModifiableRootModel(model)
      val (committedEntry1, committedEntry2) = dropModuleSourceEntry(committed, 2)
      assertThat((committedEntry1 as LibraryOrderEntry).library).isEqualTo(library)
      assertThat((committedEntry2 as LibraryOrderEntry).library).isEqualTo(library)
    }

    run {
      val model = createModifiableModel(module)
      (model.orderEntries[2] as LibraryOrderEntry).scope = DependencyScope.RUNTIME
      model.removeOrderEntry(model.orderEntries[1])
      val committed = commitModifiableRootModel(model)
      val committedEntry = dropModuleSourceEntry(committed, 1).single() as LibraryOrderEntry
      assertThat(committedEntry.scope).isEqualTo(DependencyScope.RUNTIME)
      assertThat(committedEntry.library).isEqualTo(library)
    }
  }

  @Test
  fun `remove referenced library`() {
    val library = projectModel.addProjectLevelLibrary("foo")
    run {
      val model = createModifiableModel(module)
      model.addLibraryEntry(library)
      commitModifiableRootModel(model)
    }
    runWriteActionAndWait { projectModel.projectLibraryTable.removeLibrary(library) }

    run {
      val libraryEntry = dropModuleSourceEntry(ModuleRootManager.getInstance(module), 1).single() as LibraryOrderEntry
      assertThat(libraryEntry.library).isNull()
      assertThat(libraryEntry.libraryName).isEqualTo("foo")
    }

    val newLibrary = projectModel.addProjectLevelLibrary("foo")
    run {
      val libraryEntry = dropModuleSourceEntry(ModuleRootManager.getInstance(module), 1).single() as LibraryOrderEntry
      assertThat(libraryEntry.library).isEqualTo(newLibrary)
    }
  }

  @Test
  fun `add invalid library`() {
    run {
      val model = createModifiableModel(module)
      model.addInvalidLibrary("foo", LibraryTablesRegistrar.PROJECT_LEVEL)
      val committed = commitModifiableRootModel(model)
      val libraryEntry = dropModuleSourceEntry(committed, 1).single() as LibraryOrderEntry
      assertThat(libraryEntry.library).isNull()
      assertThat(libraryEntry.libraryName).isEqualTo("foo")
    }

    val library = projectModel.addProjectLevelLibrary("foo")
    run {
      val libraryEntry = dropModuleSourceEntry(ModuleRootManager.getInstance(module), 1).single() as LibraryOrderEntry
      assertThat(libraryEntry.library).isEqualTo(library)
    }
  }
}