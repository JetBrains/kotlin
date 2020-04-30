// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.roots

import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.roots.libraries.LibraryTable
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar

class ApplicationLevelLibrariesInRootModelTest : LibrariesFromLibraryTableInRootModelTestCase() {
  override val libraryTable: LibraryTable
    get() = LibraryTablesRegistrar.getInstance().libraryTable

  override fun createLibrary(name: String): Library = projectModel.addApplicationLevelLibrary(name)
}