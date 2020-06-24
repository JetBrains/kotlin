// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.analysis.problemsView.toolWindow

import com.intellij.openapi.util.text.StringUtil.naturalCompare

internal data class NodeComparator(
  private val sortFoldersFirst: Boolean,
  private val sortBySeverity: Boolean,
  private val sortByName: Boolean)
  : Comparator<Node?> {

  override fun compare(node1: Node?, node2: Node?): Int {
    if (node1 === node2) return 0
    if (node1 == null) return +1
    if (node2 == null) return -1
    if (node1 is ProblemNode && node2 is ProblemNode) return compare(node1, node2)
    if (node1 is ProblemNode) return -1 // problem node before other nodes
    if (node2 is ProblemNode) return +1
    if (sortFoldersFirst && node1 is FileNode && node2 is FileNode) {
      if (node1.file.isDirectory && !node2.file.isDirectory) return -1
      if (!node1.file.isDirectory && node2.file.isDirectory) return +1
    }
    return naturalCompare(node1.name, node2.name)
  }

  private fun compare(problem1: ProblemNode, problem2: ProblemNode): Int {
    if (sortBySeverity) {
      val result = problem2.severity.compareTo(problem1.severity)
      if (result != 0) return result
    }
    return if (sortByName) {
      val result = naturalCompare(problem1.description, problem2.description)
      if (result != 0) result else problem1.offset.compareTo(problem2.offset)
    }
    else {
      val result = problem1.offset.compareTo(problem2.offset)
      if (result != 0) result else naturalCompare(problem1.description, problem2.description)
    }
  }
}
