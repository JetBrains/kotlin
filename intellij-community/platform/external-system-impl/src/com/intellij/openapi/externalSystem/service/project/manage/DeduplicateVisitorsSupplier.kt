// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.service.project.manage

import com.intellij.openapi.externalSystem.model.Key
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.project.LibraryData
import com.intellij.openapi.externalSystem.model.project.LibraryDependencyData
import com.intellij.openapi.externalSystem.model.project.ModuleData
import com.intellij.openapi.externalSystem.model.project.ModuleDependencyData
import com.intellij.util.containers.HashSetInterner
import com.intellij.util.containers.Interner
import java.util.function.Function

class DeduplicateVisitorsSupplier {
  private val myModuleData: Interner<ModuleData> = HashSetInterner()
  private val myLibraryData: Interner<LibraryData> = HashSetInterner()

  fun getVisitor(key: Key<*>): Function<*,*>? = when (key) {
    ProjectKeys.LIBRARY_DEPENDENCY -> Function { dep: LibraryDependencyData? -> visit(dep) }
    ProjectKeys.MODULE_DEPENDENCY -> Function { dep: ModuleDependencyData? -> visit(dep) }
    else -> null
  }

  fun visit(data: LibraryDependencyData?): LibraryDependencyData? {
    if (data == null) {
      return null
    }
    data.ownerModule = myModuleData.intern(data.ownerModule)
    data.target = myLibraryData.intern(data.target)
    return data
  }

  fun visit(data: ModuleDependencyData?): ModuleDependencyData? {
    if (data == null) {
      return null
    }
    data.ownerModule = myModuleData.intern(data.ownerModule)
    data.target = myModuleData.intern(data.target)
    return data
  }
}

