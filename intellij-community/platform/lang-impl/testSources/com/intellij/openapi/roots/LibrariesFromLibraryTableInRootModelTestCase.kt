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
  protected open fun createLibrary(name: String, model: LibraryTable.ModifiableModel) = model.createLibrary(name) as LibraryEx

  @Test
  fun `add edit remove library`() {
    val library = createLibrary("foo")
    run {
      val model = createModifiableModel(module)
      val libraryEntry = model.addLibraryEntry(library)
      assertThat(libraryEntry.isModuleLevel).isFalse()
      assertThat(libraryEntry.libraryName).isEqualTo("foo")
      assertThat(libraryEntry.presentableName).isEqualTo("foo")
      assertThat(libraryEntry.library).isEqualTo(library)
      assertThat(libraryEntry.ownerModule).isEqualTo(module)
      assertThat(libraryEntry.libraryLevel).isEqualTo(libraryTable.tableLevel)
      assertThat(libraryEntry.isValid).isTrue()
      assertThat(libraryEntry.isSynthetic).isFalse()
      assertThat(model.findLibraryOrderEntry(library)).isEqualTo(libraryEntry)
      assertThat((library as LibraryEx).isDisposed).isFalse()
      val committed = commitModifiableRootModel(model)
      val committedEntry = dropModuleSourceEntry(committed, 1).single() as LibraryOrderEntry
      assertThat(committedEntry.scope).isEqualTo(DependencyScope.COMPILE)
      assertThat(committedEntry.isExported).isFalse()
      assertThat(committedEntry.isValid).isTrue()
      assertThat(committedEntry.isSynthetic).isFalse()
      assertThat(committedEntry.presentableName).isEqualTo("foo")
      assertThat(committedEntry.ownerModule).isEqualTo(module)
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
      val entry = model.addInvalidLibrary("foo", libraryTable.tableLevel)
      assertThat(entry.isValid).isFalse()
      assertThat(entry.isSynthetic).isFalse()
      assertThat(entry.presentableName).isEqualTo("foo")
      val committed = commitModifiableRootModel(model)
      val libraryEntry = dropModuleSourceEntry(committed, 1).single() as LibraryOrderEntry
      assertThat(libraryEntry.library).isNull()
      assertThat(libraryEntry.libraryName).isEqualTo("foo")
      assertThat(libraryEntry.isValid).isFalse()
      assertThat(libraryEntry.isSynthetic).isFalse()
      assertThat(libraryEntry.presentableName).isEqualTo("foo")
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
  fun `rename library`() {
    val a = createLibrary("a")
    ModuleRootModificationUtil.addDependency(module, a)
    projectModel.renameLibrary(a, "b")
    val libraryEntry = dropModuleSourceEntry(ModuleRootManager.getInstance(module), 1).single() as LibraryOrderEntry
    assertThat(libraryEntry.library).isEqualTo(a)
    assertThat(libraryEntry.libraryName).isEqualTo("b")
  }
  
  @Test
  fun `rename library and commit after committing root model`() {
    val a = createLibrary("a")
    val model = createModifiableModel(module)
    model.addLibraryEntry(a)
    val libModel = a.modifiableModel
    libModel.name = "b"
    val libraryEntry = dropModuleSourceEntry(model, 1).single() as LibraryOrderEntry
    assertThat(libraryEntry.library).isEqualTo(a)
    assertThat(libraryEntry.libraryName).isEqualTo("a")
    val committed = commitModifiableRootModel(model)
    val committedEntry1 = dropModuleSourceEntry(committed, 1).single() as LibraryOrderEntry
    assertThat(committedEntry1.library).isEqualTo(a)
    assertThat(committedEntry1.libraryName).isEqualTo("a")
    runWriteActionAndWait { libModel.commit() }
    val committedEntry2 = dropModuleSourceEntry(committed, 1).single() as LibraryOrderEntry
    assertThat(committedEntry2.library).isEqualTo(a)
    assertThat(committedEntry2.libraryName).isEqualTo("b")
  }
  
  @Test
  fun `rename library before committing root model`() {
    //this test doesn't work under the new project model because we didn't update references to library in created modifiable root models
    // however it seems that such scenarios don't happen in production: library may be renamed after creating ModifiableRootModel only
    // in Project Structure dialog, but it commits ModifiableRootModel before committing libraries, so everything works properly.
    ProjectModelRule.ignoreTestUnderWorkspaceModel()
    val a = createLibrary("a")
    val model = createModifiableModel(module)
    model.addLibraryEntry(a)
    projectModel.renameLibrary(a, "b")
    val libraryEntry = dropModuleSourceEntry(model, 1).single() as LibraryOrderEntry
    assertThat(libraryEntry.library).isEqualTo(a)
    assertThat(libraryEntry.libraryName).isEqualTo("b")
    val committed = commitModifiableRootModel(model)
    val committedEntry = dropModuleSourceEntry(committed, 1).single() as LibraryOrderEntry
    assertThat(committedEntry.library).isEqualTo(a)
    assertThat(committedEntry.libraryName).isEqualTo("b")
  }

  @Test
  fun `add invalid library and rename library to that name`() {
    val library = createLibrary("foo")
    val model = createModifiableModel(module)
    model.addInvalidLibrary("bar", libraryTable.tableLevel)
    commitModifiableRootModel(model)

    projectModel.renameLibrary(library, "bar")

    val libraryEntry = dropModuleSourceEntry(ModuleRootManager.getInstance(module), 1).single() as LibraryOrderEntry
    assertThat(libraryEntry.library).isEqualTo(library)
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
    val a = createLibrary("a", libraryTableModel)
    run {
      val model = createModifiableModel(module)
      val entry = model.addLibraryEntry(a)
      assertThat(entry.libraryName).isEqualTo("a")
      val committed = commitModifiableRootModel(model)
      val libraryEntry = dropModuleSourceEntry(committed, 1).single() as LibraryOrderEntry
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
    val a = createLibrary("a", libraryTableModel)
    val model = createModifiableModel(module)
    val entry = model.addLibraryEntry(a)
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
    val a = createLibrary("a", libraryTableModel)
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
      assertThat(libraryEntry.libraryName).isEqualTo("a")
    }
    runWriteActionAndWait { libraryTableModel.commit() }
    run {
      val libraryEntry = dropModuleSourceEntry(ModuleRootManager.getInstance(module), 1).single() as LibraryOrderEntry
      assertThat(libraryEntry.library).isEqualTo(a)
    }
  }
}