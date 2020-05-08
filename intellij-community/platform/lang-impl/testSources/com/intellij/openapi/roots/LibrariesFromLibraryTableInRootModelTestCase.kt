// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.roots

import com.intellij.openapi.application.runWriteActionAndWait
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.impl.RootConfigurationAccessor
import com.intellij.openapi.roots.impl.libraries.LibraryEx
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.roots.libraries.LibraryTable
import com.intellij.testFramework.ApplicationRule
import com.intellij.testFramework.assertions.Assertions.assertThat
import com.intellij.testFramework.rules.ProjectModelRule
import org.junit.Before
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test

abstract class LibrariesFromLibraryTableInRootModelTestCase {
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

  protected abstract fun createLibrary(name: String): Library
  protected abstract val libraryTable: LibraryTable

  @Test
  fun `add edit remove library`() {
    val library = createLibrary("foo")
    run {
      val model = createModifiableModel(module)
      val libraryEntry = model.addLibraryEntry(library)
      assertThat(libraryEntry.isModuleLevel).isFalse()
      assertThat(libraryEntry.libraryName).isEqualTo("foo")
      assertThat(libraryEntry.library).isEqualTo(library)
      assertThat(libraryEntry.libraryLevel).isEqualTo(libraryTable.tableLevel)
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
  fun `edit and commit library before committing root model`() {
    val library = createLibrary("foo")
    val model = createModifiableModel(module)
    val libraryEntry = model.addLibraryEntry(library)
    assertThat(libraryEntry.library).isEqualTo(library)
    val libraryModel = library.modifiableModel
    val libRoot = projectModel.baseProjectDir.newVirtualDirectory("lib")
    libraryModel.addRoot(libRoot, OrderRootType.CLASSES)
    runWriteActionAndWait { libraryModel.commit() }
    val committed = commitModifiableRootModel(model)
    val committedEntry = dropModuleSourceEntry(committed, 1).single() as LibraryOrderEntry
    assertThat(committedEntry.library).isEqualTo(library)
    assertThat(committedEntry.getFiles(OrderRootType.CLASSES).single()).isEqualTo(libRoot)
  }

  @Test
  fun `edit library before committing root model and commit after that`() {
    val library = createLibrary("foo")
    val model = createModifiableModel(module)
    val libraryEntry = model.addLibraryEntry(library)
    assertThat(libraryEntry.library).isEqualTo(library)
    val libraryModel = library.modifiableModel
    val libRoot = projectModel.baseProjectDir.newVirtualDirectory("lib")
    libraryModel.addRoot(libRoot, OrderRootType.CLASSES)

    val committed = commitModifiableRootModel(model)
    val committedEntry1 = dropModuleSourceEntry(committed, 1).single() as LibraryOrderEntry
    assertThat(committedEntry1.getFiles(OrderRootType.CLASSES)).isEmpty()
    assertThat(committedEntry1.library).isEqualTo(library)

    runWriteActionAndWait { libraryModel.commit() }
    val committedEntry2 = dropModuleSourceEntry(committed, 1).single() as LibraryOrderEntry
    assertThat(committedEntry2.library).isEqualTo(library)
    assertThat(committedEntry2.getFiles(OrderRootType.CLASSES).single()).isEqualTo(libRoot)
  }

  @Test
  fun `add same library twice`() {
    val library = createLibrary("foo")
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
    val library = createLibrary("foo")
    run {
      val model = createModifiableModel(module)
      model.addLibraryEntry(library)
      commitModifiableRootModel(model)
    }
    runWriteActionAndWait { libraryTable.removeLibrary(library) }

    run {
      val libraryEntry = dropModuleSourceEntry(ModuleRootManager.getInstance(module), 1).single() as LibraryOrderEntry
      assertThat(libraryEntry.library).isNull()
      assertThat(libraryEntry.libraryName).isEqualTo("foo")
    }

    val newLibrary = createLibrary("foo")
    run {
      val libraryEntry = dropModuleSourceEntry(ModuleRootManager.getInstance(module), 1).single() as LibraryOrderEntry
      assertThat(libraryEntry.library).isEqualTo(newLibrary)
    }
  }

  @Test
  fun `add invalid library`() {
    run {
      val model = createModifiableModel(module)
      model.addInvalidLibrary("foo", libraryTable.tableLevel)
      val committed = commitModifiableRootModel(model)
      val libraryEntry = dropModuleSourceEntry(committed, 1).single() as LibraryOrderEntry
      assertThat(libraryEntry.library).isNull()
      assertThat(libraryEntry.libraryName).isEqualTo("foo")
    }

    val library = createLibrary("foo")
    run {
      val libraryEntry = dropModuleSourceEntry(ModuleRootManager.getInstance(module), 1).single() as LibraryOrderEntry
      assertThat(libraryEntry.library).isEqualTo(library)
    }
  }

  @Test
  fun `change order`() {
    val a = createLibrary("a")
    val b = createLibrary("b")
    run {
      val model = createModifiableModel(module)
      model.addLibraryEntry(a)
      model.addLibraryEntry(b)
      val oldOrder = model.orderEntries
      assertThat(oldOrder).hasSize(3)
      assertThat((oldOrder[1] as LibraryOrderEntry).libraryName).isEqualTo("a")
      assertThat((oldOrder[2] as LibraryOrderEntry).libraryName).isEqualTo("b")
      val newOrder = arrayOf(oldOrder[0], oldOrder[2], oldOrder[1])
      model.rearrangeOrderEntries(newOrder)
      assertThat((model.orderEntries[1] as LibraryOrderEntry).libraryName).isEqualTo("b")
      assertThat((model.orderEntries[2] as LibraryOrderEntry).libraryName).isEqualTo("a")
      val committed = commitModifiableRootModel(model)
      assertThat((committed.orderEntries[1] as LibraryOrderEntry).libraryName).isEqualTo("b")
      assertThat((committed.orderEntries[2] as LibraryOrderEntry).libraryName).isEqualTo("a")
    }

    run {
      val model = createModifiableModel(module)
      val oldOrder = model.orderEntries
      assertThat((oldOrder[1] as LibraryOrderEntry).libraryName).isEqualTo("b")
      assertThat((oldOrder[2] as LibraryOrderEntry).libraryName).isEqualTo("a")
      val newOrder = arrayOf(oldOrder[0], oldOrder[2], oldOrder[1])
      model.rearrangeOrderEntries(newOrder)
      assertThat((model.orderEntries[1] as LibraryOrderEntry).libraryName).isEqualTo("a")
      assertThat((model.orderEntries[2] as LibraryOrderEntry).libraryName).isEqualTo("b")
      model.removeOrderEntry(model.orderEntries[1])
      val committed = commitModifiableRootModel(model)
      val libraryEntry = dropModuleSourceEntry(committed, 1).single() as LibraryOrderEntry
      assertThat(libraryEntry.library).isEqualTo(b)
    }
  }

  @Test
  fun `dispose model without committing`() {
    val a = createLibrary("a")
    val model = createModifiableModel(module)
    val entry = model.addLibraryEntry(a)
    assertThat(entry.library).isEqualTo(a)
    model.dispose()
    dropModuleSourceEntry(ModuleRootManager.getInstance(module), 0)
  }

  @Test
  fun `add not yet committed library and commit root model`() {
    val libraryTableModel = libraryTable.modifiableModel
    val a = libraryTableModel.createLibrary("a")
    run {
      val model = createModifiableModel(module)
      val entry = model.addLibraryEntry(a)
      assertThat(entry.library).isEqualTo(a)
      assertThat(entry.libraryName).isEqualTo("a")
      val committed = commitModifiableRootModel(model)
      val libraryEntry = dropModuleSourceEntry(committed, 1).single() as LibraryOrderEntry
      assertThat(libraryEntry.library).isEqualTo(a)
      assertThat(libraryEntry.libraryName).isEqualTo("a")
    }
    runWriteActionAndWait { libraryTableModel.commit() }
    run {
      val libraryEntry = dropModuleSourceEntry(ModuleRootManager.getInstance(module), 1).single() as LibraryOrderEntry
      assertThat(libraryEntry.library).isEqualTo(a)
    }
  }

  @Test
  fun `add not yet committed library and commit before committing root model`() {
    val libraryTableModel = libraryTable.modifiableModel
    val a = libraryTableModel.createLibrary("a")
    val model = createModifiableModel(module)
    val entry = model.addLibraryEntry(a)
    assertThat(entry.library).isEqualTo(a)
    assertThat(entry.libraryName).isEqualTo("a")
    runWriteActionAndWait { libraryTableModel.commit() }
    val committed = commitModifiableRootModel(model)
    val committedEntry = dropModuleSourceEntry(committed, 1).single() as LibraryOrderEntry
    assertThat(committedEntry.library).isEqualTo(a)
    assertThat(committedEntry.libraryName).isEqualTo("a")
  }

  @Test
  fun `add not yet committed library with configuration accessor`() {
    val libraryTableModel = libraryTable.modifiableModel
    val a = libraryTableModel.createLibrary("a")
    run {
      val model = createModifiableModel(module, object : RootConfigurationAccessor() {
        override fun getLibrary(library: Library?, libraryName: String?, libraryLevel: String?): Library? {
          return if (libraryName == "a") a else library
        }
      })
      val entry = model.addLibraryEntry(a)
      assertThat(entry.library).isEqualTo(a)
      val committed = commitModifiableRootModel(model)
      val libraryEntry = dropModuleSourceEntry(committed, 1).single() as LibraryOrderEntry
      assertThat(libraryEntry.library).isEqualTo(a)
      assertThat(libraryEntry.libraryName).isEqualTo("a")
    }
    runWriteActionAndWait { libraryTableModel.commit() }
    run {
      val libraryEntry = dropModuleSourceEntry(ModuleRootManager.getInstance(module), 1).single() as LibraryOrderEntry
      assertThat(libraryEntry.library).isEqualTo(a)
    }
  }
}