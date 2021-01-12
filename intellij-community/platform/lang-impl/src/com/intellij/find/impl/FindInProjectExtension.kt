// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.find.impl

import com.intellij.find.FindModel
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.psi.search.scope.packageSet.NamedScope
import org.jetbrains.annotations.ApiStatus

interface FindInProjectExtension {
  companion object {
    @JvmField
    val EP_NAME = ExtensionPointName.create<FindInProjectExtension>("com.intellij.findInProjectExtension")
  }

  /**
   * Returns true if model was changed by extension
   */
  fun initModelFromContext(model: FindModel, dataContext: DataContext): Boolean {
    return false
  }

  @ApiStatus.Experimental
  fun getFilteredNamedScopes(project: Project): List<NamedScope> {
    return listOf()
  }
}