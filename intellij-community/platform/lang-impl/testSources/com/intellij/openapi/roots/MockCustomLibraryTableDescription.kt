// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.roots

import com.intellij.openapi.roots.libraries.CustomLibraryTableDescription
import com.intellij.openapi.roots.libraries.LibraryTablePresentation

class MockCustomLibraryTableDescription : CustomLibraryTableDescription {
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
}