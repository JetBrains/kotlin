// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.analysis.problemsView.toolWindow

import com.intellij.codeInsight.problems.WolfTheProblemSolverImpl
import com.intellij.icons.AllIcons.Toolwindows
import com.intellij.openapi.actionSystem.ToggleOptionAction.Option
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.problems.ProblemListener
import com.intellij.problems.WolfTheProblemSolver

internal class ProjectErrorsPanel(project: Project, state: ProblemsViewState)
  : ProblemsViewPanel(project, state), ProblemListener {

  private val watchers = mutableMapOf<VirtualFile, HighlightingWatcher>()
  private val root = Root(this)

  init {
    treeModel.root = root
    tree.emptyText.text = ProblemsViewBundle.message("problems.view.project.empty")
    project.messageBus.connect(this)
      .subscribe(ProblemListener.TOPIC, this)
    val problems = WolfTheProblemSolver.getInstance(project) as? WolfTheProblemSolverImpl
    problems?.processProblemFiles {
      problemsAppeared(it)
      true
    }
  }

  companion object {
    private val LOG = Logger.getInstance(ProjectErrorsPanel::class.java)
  }

  override fun getDisplayName() = ProblemsViewBundle.message("problems.view.project")
  override fun getSortFoldersFirst(): Option? = null
  override fun getSortBySeverity(): Option? = null

  override fun getToolWindowIcon(count: Int) = if (count > 0) Toolwindows.ToolWindowProblems else Toolwindows.ToolWindowProblemsEmpty

  override fun problemsAppeared(file: VirtualFile) {
    LOG.debug("problemsAppeared: ", file)
    synchronized(watchers) { watchers.computeIfAbsent(file) { HighlightingWatcher(root, it) } }
  }

  override fun problemsChanged(file: VirtualFile) {
    LOG.debug("problemsChanged: ", file)
  }

  override fun problemsDisappeared(file: VirtualFile) {
    LOG.debug("problemsDisappeared: ", file)
    val watcher = synchronized(watchers) { watchers.remove(file) } ?: return
    Disposer.dispose(watcher) // removes a markup model listener
    root.removeProblems(file, *watcher.getProblems().toTypedArray())
  }
}
