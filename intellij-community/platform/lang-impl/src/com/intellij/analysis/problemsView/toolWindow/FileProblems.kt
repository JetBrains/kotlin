// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.analysis.problemsView.toolWindow

internal class FileProblems {
  private val problems = mutableSetOf<Problem>()
  private val nodes = mutableMapOf<Problem, ProblemNode>()

  fun getNodes(parent: FileNode): Collection<ProblemNode> {
    problems.forEach { problem ->
      var node = nodes[problem]
      if (node == null || parent !== node.parentDescriptor) {
        node = ProblemNode(parent, problem)
        nodes[problem] = node
      }
    }
    return nodes.values
  }

  fun findNode(problem: Problem) = nodes[problem]

  fun add(problem: Problem) = problems.add(problem)

  fun remove(problem: Problem): Boolean {
    if (!problems.remove(problem)) return false
    nodes.remove(problem)
    return true
  }

  fun clear() {
    problems.clear()
    nodes.clear()
  }

  fun count() = problems.size

  fun count(severity: Severity) = problems.count { it.severity == severity }
}
