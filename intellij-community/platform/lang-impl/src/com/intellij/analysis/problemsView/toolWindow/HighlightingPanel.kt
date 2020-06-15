// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.analysis.problemsView.toolWindow

import com.intellij.icons.AllIcons.Toolwindows
import com.intellij.ide.TreeExpander
import com.intellij.openapi.actionSystem.ToggleOptionAction.Option
import com.intellij.openapi.application.Experiments
import com.intellij.openapi.application.ModalityState.stateForComponent
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.ex.EditorMarkupModel
import com.intellij.openapi.editor.ex.RangeHighlighterEx
import com.intellij.openapi.editor.markup.AnalyzerStatus
import com.intellij.openapi.editor.markup.AnalyzingType.COMPLETE
import com.intellij.openapi.fileEditor.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.problems.WolfTheProblemSolver
import com.intellij.util.SingleAlarm
import com.intellij.util.ui.tree.TreeUtil.promiseSelectFirstLeaf
import javax.swing.Icon

internal class HighlightingPanel(project: Project, state: ProblemsViewState)
  : ProblemsViewPanel(project, state), FileEditorManagerListener {

  private val statusUpdateAlarm = SingleAlarm(Runnable(this::updateStatus), 200, stateForComponent(this), this)

  init {
    tree.showsRootHandles = false
    project.messageBus.connect(this)
      .subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, this)
  }

  override fun getDisplayName() = ProblemsViewBundle.message("problems.view.highlighting")
  override fun getSortFoldersFirst(): Option? = null
  override fun getTreeExpander(): TreeExpander? = null

  override fun getToolWindowIcon(count: Int): Icon? {
    if (Experiments.getInstance().isFeatureEnabled("problems.view.project.errors.enabled")) return null
    val root = treeModel.root as? HighlightingFileRoot
    val problem = root?.file?.let { WolfTheProblemSolver.getInstance(project).isProblemFile(it) }
    return if (problem == true) Toolwindows.ToolWindowProblems else Toolwindows.ToolWindowProblemsEmpty
  }

  override fun selectionChangedTo(selected: Boolean) {
    super.selectionChangedTo(selected)
    if (selected) updateCurrentFile()
  }

  fun selectHighlighter(highlighter: RangeHighlighterEx) {
    val root = treeModel.root as? HighlightingFileRoot
    root?.findProblemNode(highlighter)?.let { select(it) }
  }

  fun requestStatusUpdating() {
    statusUpdateAlarm.cancelAndRequest(forceRun = true)
    updateToolWindowContent()
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
    requestStatusUpdating()
  }

  private fun findCurrentFile(): VirtualFile? {
    if (project.isDisposed) return null
    val fileEditor = FileEditorManager.getInstance(project)?.selectedEditor ?: return null
    val file = fileEditor.file
    if (file != null) return file
    val textEditor = fileEditor as? TextEditor ?: return null
    return FileDocumentManager.getInstance().getFile(textEditor.editor.document)
  }

  private fun updateStatus() {
    val root = treeModel.root as? HighlightingFileRoot
    val status = root?.file?.let { findAnalyzerStatus(it) }
    when {
      root == null -> {
        tree.emptyText.text = ProblemsViewBundle.message("problems.view.highlighting.no.selected.file")
      }
      status == null -> {
        val name = with(root.file) { presentableName ?: name }
        tree.emptyText.text = ProblemsViewBundle.message("problems.view.highlighting.problems.analyzing", name)
        statusUpdateAlarm.cancelAndRequest()
      }
      status.title.isEmpty() || status.title == "No problems found" -> {
        val name = with(root.file) { presentableName ?: name }
        tree.emptyText.text = ProblemsViewBundle.message("problems.view.highlighting.problems.not.found", name)
        if (state.hideBySeverity.isNotEmpty()) {
          tree.emptyText.appendLine(ProblemsViewBundle.message("problems.view.highlighting.problems.not.found.filter"))
        }
      }
      else -> {
        tree.emptyText.text = status.title
        if (status.details.isNotEmpty()) tree.emptyText.appendLine(status.details)
        statusUpdateAlarm.cancelAndRequest()
      }
    }
  }

  private fun findAnalyzerStatus(file: VirtualFile): AnalyzerStatus? {
    val document = ProblemsView.getDocument(project, file) ?: return null
    val editor = EditorFactory.getInstance().editors(document, project).findFirst().orElse(null) ?: return null
    val model = editor.markupModel as? EditorMarkupModel ?: return null
    val status = model.errorStripeRenderer?.getStatus(editor) ?: return null
    return if (status.analyzingType == COMPLETE) status else null
  }
}
