// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.analysis.problemsView.toolWindow

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.util.treeView.PresentableNodeDescriptor
import com.intellij.openapi.project.Project
import com.intellij.ui.tree.LeafState
import com.intellij.ui.tree.TreePathUtil.pathToCustomNode
import java.util.Collections.emptyList

internal abstract class Node : PresentableNodeDescriptor<Node?>, LeafState.Supplier {
  protected constructor(project: Project) : super(project, null)
  protected constructor(parent: Node) : super(parent.project, parent)

  protected abstract fun update(project: Project, presentation: PresentationData)

  abstract override fun getName(): String

  override fun toString() = name

  open fun getChildren(): Collection<Node> = emptyList()

  override fun getElement() = this

  override fun update(presentation: PresentationData) {
    if (myProject == null || myProject.isDisposed) return
    update(myProject, presentation)
  }

  fun getPath() = pathToCustomNode(this, { node: Node? -> node?.getParent(Node::class.java) })!!

  fun <T> getParent(type: Class<T>): T? {
    val parent = parentDescriptor ?: return null
    @Suppress("UNCHECKED_CAST")
    if (type.isInstance(parent)) return parent as T
    throw IllegalStateException("unexpected node " + parent.javaClass)
  }

  fun <T> findAncestor(type: Class<T>): T? {
    var parent = parentDescriptor
    while (parent != null) {
      @Suppress("UNCHECKED_CAST")
      if (type.isInstance(parent)) return parent as T
      parent = parent.parentDescriptor
    }
    return null
  }
}
