// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.analysis.problemsView.toolWindow

import com.intellij.openapi.vfs.VirtualFile

internal class FileProblems(val file: VirtualFile) {
  private val problems = mutableSetOf<Problem>()
  private val nodes = mutableMapOf<Problem, ProblemNode>()
  private var fileNode: FileNode? = null

  fun getFileNode(parent: Node): FileNode {
    val oldNode = fileNode
    if (oldNode?.parentDescriptor === parent) return oldNode
    val newNode = FileNode(parent, file)
    fileNode = newNode
    nodes.clear()
    return newNode
  }

  private fun getProblemNode(parent: FileNode, problem: Problem): ProblemNode {
    val oldNode = nodes[problem]
    if (oldNode?.parentDescriptor === parent) return oldNode
    val newNode = ProblemNode(parent, problem)
    nodes[problem] = newNode
    return newNode
  }

  fun getProblemNodes(): Collection<ProblemNode> {
    if (problems.isEmpty()) return emptyList()
    val parent = fileNode ?: return emptyList()
    return problems.map { getProblemNode(parent, it) }
  }

  fun findProblemNode(problem: Problem): ProblemNode? {
    val parent = fileNode ?: return null
    return getProblemNode(parent, problem)
  }

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
}
