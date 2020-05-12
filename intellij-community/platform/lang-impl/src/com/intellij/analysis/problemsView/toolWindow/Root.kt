// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.analysis.problemsView.toolWindow

import com.intellij.ide.projectView.PresentationData
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.tree.LeafState

internal abstract class Root(val panel: ProblemsViewPanel) : Node(panel.project), Disposable {

  override fun dispose() = Unit

  override fun getLeafState() = LeafState.NEVER

  override fun getName() = panel.displayName

  override fun update(project: Project, presentation: PresentationData) = Unit

  abstract override fun getChildren(): Collection<Node>

  abstract fun getChildren(file: VirtualFile): Collection<Node>

  open fun getProblemsCount() = 0

  open fun getProblemsCount(file: VirtualFile, severity: Severity) = 0
}
