// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.view

import com.intellij.openapi.project.Project
import com.intellij.psi.search.scope.TestsScope
import com.intellij.ui.FileColorManager
import com.intellij.ui.treeStructure.SimpleTree
import com.intellij.util.ui.tree.TreeUtil
import java.awt.Color

class ExternalProjectTree(project: Project) : SimpleTree() {
  private val colorManager = FileColorManager.getInstance(project)

  override fun isFileColorsEnabled() = true

  override fun getFileColorFor(value: Any?): Color? {
    val node = TreeUtil.getUserObject(value)
    if (node is TaskNode && node.isTest) {
      return colorManager.getScopeColor(TestsScope.NAME)
    }
    return null
  }
}