// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.roots

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.roots.libraries.*
import com.intellij.testFramework.DisposableRule
import org.junit.Before
import org.junit.Rule

class LibrariesFromCustomTableInRootModelTest : LibrariesFromLibraryTableInRootModelTestCase() {
  @Rule
  @JvmField
  val disposableRule = DisposableRule()

  override val libraryTable: LibraryTable
    get() = LibraryTablesRegistrar.getInstance().getCustomLibraryTableByLevel("mock")!!

  @Before
  fun registerCustomLibraryTable() {
    ExtensionPointName.create<CustomLibraryTableDescription>("com.intellij.customLibraryTable").point.registerExtension(object : CustomLibraryTableDescription {
      override fun getPresentation(): LibraryTablePresentation {
        return object : LibraryTablePresentation() {
          override fun getLibraryTableEditorTitle(): String = "Mock"
          override fun getDescription(): String = "Mock"
          override fun getDisplayName(plural: Boolean): String = "Mock"
        }
      }

      override fun getTableLevel(): String {
        return "mock"
      }
    }, disposableRule.disposable)
  }

  override fun createLibrary(name: String): Library = projectModel.addLibrary(name, libraryTable)
}