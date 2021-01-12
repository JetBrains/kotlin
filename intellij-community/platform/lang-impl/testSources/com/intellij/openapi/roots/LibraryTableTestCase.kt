// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.roots

import com.intellij.openapi.application.runWriteActionAndWait
import com.intellij.openapi.roots.impl.libraries.LibraryEx
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.roots.libraries.LibraryTable
import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.ApplicationRule
import com.intellij.testFramework.DisposableRule
import com.intellij.testFramework.rules.ProjectModelRule
import org.assertj.core.api.Assertions.assertThat
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test

abstract class LibraryTableTestCase {
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
  val disposableRule = DisposableRule()

  protected abstract val libraryTable: LibraryTable
  protected abstract fun createLibrary(name: String, setup: (LibraryEx.ModifiableModelEx) -> Unit = {}): LibraryEx
  protected open fun createLibrary(name: String, model: LibraryTable.ModifiableModel) = model.createLibrary(name) as LibraryEx

  @Test
  fun `add remove library`() {
    assertThat(libraryTable.libraries).isEmpty()
    val library = createLibrary("a")
    checkConsistency()
    assertThat(libraryTable.libraries).containsExactly(library)
    assertThat(library.isDisposed).isFalse()
    runWriteActionAndWait { libraryTable.removeLibrary(library) }
    checkConsistency()
    assertThat(libraryTable.libraries).isEmpty()
    assertThat(library.isDisposed).isTrue()
  }

  @Test
  fun listener() {
    val events = ArrayList<String>()
    libraryTable.addListener(object : LibraryTable.Listener {
      override fun afterLibraryAdded(newLibrary: Library) {
        events += "added ${newLibrary.name}"
      }

      override fun afterLibraryRenamed(library: Library, oldName: String?) {
        events += "renamed ${library.name}"
      }

      override fun beforeLibraryRemoved(library: Library) {
        events += "before removed ${library.name}"
      }

      override fun afterLibraryRemoved(library: Library) {
        events += "removed ${library.name}"
      }
    })
    val library = createLibrary("a")
    assertThat(events).containsExactly("added a")
    events.clear()
    projectModel.renameLibrary(library, "b")
    assertThat(events).containsExactly("renamed b")
    events.clear()
    runWriteActionAndWait { libraryTable.removeLibrary(library) }
    assertThat(events).containsExactly("before removed b", "removed b")
  }

  @Test
  fun `remove library before committing`() {
    val library = edit {
      val library = createLibrary("a", it)
      assertThat(it.isChanged).isTrue()
      it.removeLibrary(library)
      library
    }
    assertThat(libraryTable.libraries).isEmpty()
    assertThat(library.isDisposed).isTrue()
  }

  @Test
  fun `add library and dispose model`() {
    val a = createLibrary("a")
    val model = libraryTable.modifiableModel
    val b = createLibrary("b", model)
    Disposer.dispose(model)
    assertThat(libraryTable.libraries).containsExactly(a)
    assertThat(b.isDisposed).isTrue()
    assertThat(a.isDisposed).isFalse()
  }

  @Test
  fun `rename uncommitted library`() {
    val library = edit {
      val library = createLibrary("a", it)
      val libraryModel = library.modifiableModel
      libraryModel.name = "b"
      assertThat(it.getLibraryByName("a")).isEqualTo(library)
      assertThat(it.getLibraryByName("b")).isNull()
      runWriteActionAndWait { libraryModel.commit() }
      assertThat(it.getLibraryByName("a")).isNull()
      assertThat(it.getLibraryByName("b")).isEqualTo(library)
      assertThat(libraryTable.getLibraryByName("b")).isNull()
      library
    }
    assertThat(libraryTable.getLibraryByName("b")).isEqualTo(library)
  }

  @Test
  fun `remove library and dispose model`() {
    val a = createLibrary("a")
    val model = libraryTable.modifiableModel
    model.removeLibrary(a)
    Disposer.dispose(model)
    assertThat(libraryTable.libraries).containsExactly(a)
    assertThat(a.isDisposed).isFalse()
  }

  @Test
  fun `merge add remove changes`() {
    val a = createLibrary("a")
    val model1 = libraryTable.modifiableModel
    model1.removeLibrary(a)
    val model2 = libraryTable.modifiableModel
    val b = createLibrary("b", model2)
    runWriteActionAndWait {
      model1.commit()
      model2.commit()
    }
    assertThat(libraryTable.libraries).containsExactly(b)
  }

  @Test
  fun `merge add add changes`() {
    val a = createLibrary("a")
    val model1 = libraryTable.modifiableModel
    val b = createLibrary("b", model1)
    val model2 = libraryTable.modifiableModel
    val c = createLibrary("c", model2)
    runWriteActionAndWait {
      model1.commit()
      model2.commit()
    }
    assertThat(libraryTable.libraries).containsExactlyInAnyOrder(a, b, c)
  }

  @Test
  fun `merge remove remove changes`() {
    val a = createLibrary("a")
    val b = createLibrary("b")
    val model1 = libraryTable.modifiableModel
    model1.removeLibrary(a)
    val model2 = libraryTable.modifiableModel
    model2.removeLibrary(b)
    runWriteActionAndWait {
      model1.commit()
      model2.commit()
    }
    assertThat(libraryTable.libraries).isEmpty()
  }


  private fun <T> edit(action: (LibraryTable.ModifiableModel) -> T): T{
    checkConsistency()
    val model = libraryTable.modifiableModel
    checkConsistency(model)
    val result = action(model)
    checkConsistency(model)
    runWriteActionAndWait { model.commit() }
    checkConsistency()
    return result
  }

  private fun checkConsistency() {
    val fromIterator = ArrayList<Library>()
    libraryTable.libraryIterator.forEach { fromIterator += it }
    assertThat(fromIterator).containsExactly(*libraryTable.libraries)
    for (library in libraryTable.libraries) {
      assertThat(libraryTable.getLibraryByName(library.name!!)).isEqualTo(library)
      assertThat((library as LibraryEx).isDisposed).isFalse()
    }
  }

  private fun checkConsistency(model: LibraryTable.ModifiableModel) {
    val fromIterator = ArrayList<Library>()
    model.libraryIterator.forEach { fromIterator += it }
    assertThat(fromIterator).containsExactly(*model.libraries)
    for (library in model.libraries) {
      assertThat(model.getLibraryByName(library.name!!)).isEqualTo(library)
      assertThat((library as LibraryEx).isDisposed).isFalse()
    }
  }
}