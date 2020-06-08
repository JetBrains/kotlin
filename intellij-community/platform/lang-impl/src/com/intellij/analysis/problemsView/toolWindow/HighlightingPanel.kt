// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.analysis.problemsView.toolWindow

import com.intellij.codeInsight.daemon.impl.SeverityRegistrar.getSeverityRegistrar
import com.intellij.ide.TreeExpander
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.actionSystem.ToggleOptionAction.Option
import com.intellij.openapi.editor.ex.RangeHighlighterEx
import com.intellij.openapi.fileEditor.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.profile.codeInspection.ui.SingleInspectionProfilePanel.renderSeverity
import com.intellij.util.ui.tree.TreeUtil.promiseSelectFirstLeaf

internal class HighlightingPanel(project: Project, state: ProblemsViewState)
  : ProblemsViewPanel(project, state), FileEditorManagerListener {

  init {
    tree.showsRootHandles = false
    project.messageBus.connect(this)
      .subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, this)
  }

  override fun getDisplayName() = ProblemsViewBundle.message("problems.view.highlighting")
  override fun getSortFoldersFirst(): Option? = null
  override fun getTreeExpander(): TreeExpander? = null

  override fun selectionChangedTo(selected: Boolean) {
    super.selectionChangedTo(selected)
    if (selected) updateCurrentFile()
  }

  fun selectHighlighter(highlighter: RangeHighlighterEx) {
    val root = treeModel.root as? HighlightingFileRoot
    root?.findProblemNode(highlighter)?.let { select(it) }
  }

  override fun fileOpened(manager: FileEditorManager, file: VirtualFile) = updateCurrentFile()
  override fun fileClosed(manager: FileEditorManager, file: VirtualFile) = updateCurrentFile()
  override fun selectionChanged(event: FileEditorManagerEvent) = updateCurrentFile()

  private fun updateCurrentFile() {
    val file = findCurrentFile()
    val root = treeModel.root as? HighlightingFileRoot
    if (file == null) {
      if (root == null) return
      treeModel.root = null
    }
    else {
      if (root != null && root.file == file) return
      treeModel.root = HighlightingFileRoot(this, file)
      promiseSelectFirstLeaf(tree)
    }
    updateToolWindowContent()
  }

  private fun findCurrentFile(): VirtualFile? {
    if (project.isDisposed) return null
    val fileEditor = FileEditorManager.getInstance(project)?.selectedEditor ?: return null
    val file = fileEditor.file
    if (file != null) return file
    val textEditor = fileEditor as? TextEditor ?: return null
    return FileDocumentManager.getInstance().getFile(textEditor.editor.document)
  }

  override fun getSeverityFilters() = getSeverityRegistrar(project).allSeverities.reversed()
    .filter { it != HighlightSeverity.INFO && it > HighlightSeverity.INFORMATION && it < HighlightSeverity.ERROR }
    .map { Pair(ProblemsViewBundle.message("problems.view.highlighting.severity.show", renderSeverity(it)), it.myVal) }
}
