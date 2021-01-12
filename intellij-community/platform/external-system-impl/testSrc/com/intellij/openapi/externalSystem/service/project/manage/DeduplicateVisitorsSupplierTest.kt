// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.service.project.manage

import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.ProjectKeys
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.externalSystem.model.project.*
import org.junit.Assert.assertTrue
import org.junit.Test

class DeduplicateVisitorsSupplierTest {

  @Test
  fun `test module dependency deduplication`() {
    val m1 = ModuleData("1", ProjectSystemId.IDE, "typeId", "m1", "1", "1-ext")
    val m1Dup = ModuleData("1", ProjectSystemId.IDE, "typeId", "m1", "1", "1-ext")
    val m2 = ModuleData("2", ProjectSystemId.IDE, "typeId", "m2", "2", "2-ext")
    val m2Dup = ModuleData("2", ProjectSystemId.IDE, "typeId", "m2", "2", "2-ext")

    val d1 = ModuleDependencyData(m1, m2)
    val d2 = ModuleDependencyData(m1Dup, m2Dup)

    val node1 = DataNode(ProjectKeys.MODULE_DEPENDENCY, d1, null)
    val node2 = DataNode(ProjectKeys.MODULE_DEPENDENCY, d2, null)

    val supplier = DeduplicateVisitorsSupplier()
    val visitor = supplier.getVisitor(ProjectKeys.MODULE_DEPENDENCY)

    node1.visitData(visitor)
    node2.visitData(visitor)

    val resultDep1 = node1.data
    val resultDep2 = node2.data

    assertTrue(resultDep1.ownerModule === resultDep2.ownerModule)
    assertTrue(resultDep1.target === resultDep2.target)
  }

  @Test
  fun `test library dependency deduplication`() {
    val module = ModuleData("1", ProjectSystemId.IDE, "typeId", "m1", "1", "1-ext")
    val mDup = ModuleData("1", ProjectSystemId.IDE, "typeId", "m1", "1", "1-ext")
    val lib = LibraryData(ProjectSystemId.IDE, "libraryName")
    val libDup = LibraryData(ProjectSystemId.IDE, "libraryName")

    val d1 = LibraryDependencyData(module, lib, LibraryLevel.PROJECT)
    val d2 = LibraryDependencyData(mDup, libDup, LibraryLevel.PROJECT)

    val node1 = DataNode(ProjectKeys.LIBRARY_DEPENDENCY, d1, null)
    val node2 = DataNode(ProjectKeys.LIBRARY_DEPENDENCY, d2, null)

    val supplier = DeduplicateVisitorsSupplier()
    val visitor = supplier.getVisitor(ProjectKeys.LIBRARY_DEPENDENCY)

    node1.visitData(visitor)
    node2.visitData(visitor)

    val resultDep1 = node1.data
    val resultDep2 = node2.data

    assertTrue(resultDep1.ownerModule === resultDep2.ownerModule)
    assertTrue(resultDep1.target === resultDep2.target)
  }
}