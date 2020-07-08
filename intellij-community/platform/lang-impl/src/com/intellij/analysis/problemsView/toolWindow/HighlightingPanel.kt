// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.analysis.problemsView.toolWindow

import com.intellij.icons.AllIcons.Toolwindows
import com.intellij.ide.PowerSaveMode
import com.intellij.ide.TreeExpander
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.ToggleOptionAction.Option
import com.intellij.openapi.application.ApplicationManager.getApplication
import com.intellij.openapi.application.Experiments
import com.intellij.openapi.application.ModalityState.stateForComponent
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.ex.EditorMarkupModel
import com.intellij.openapi.editor.ex.RangeHighlighterEx
import com.intellij.openapi.editor.markup.AnalyzingType
import com.intellij.openapi.fileEditor.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.SingleAlarm
import com.intellij.util.ui.tree.TreeUtil.promiseSelectFirstLeaf
import javax.swing.Icon

internal class HighlightingPanel(project: Project, state: ProblemsViewState)
  : ProblemsViewPanel(project, state), FileEditorManagerListener, PowerSaveMode.Listener {

  private val statusUpdateAlarm = SingleAlarm(Runnable(this::updateStatus), 200, stateForComponent(this), this)
  private var previousStatus: Status? = null

  init {
    tree.showsRootHandles = false
    project.messageBus.connect(this)
      .subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, this)
    getApplication().messageBus.connect(this)
      .subscribe(PowerSaveMode.TOPIC, this)
  }

  override fun getDisplayName() = ProblemsViewBundle.message("problems.view.highlighting")
  override fun getSortFoldersFirst(): Option? = null
  override fun getTreeExpander(): TreeExpander? = null

  override fun getData(dataId: String): Any? {
    val root = treeModel.root as? HighlightingFileRoot
    if (CommonDataKeys.VIRTUAL_FILE.`is`(dataId)) return root?.file
    return super.getData(dataId)
  }

  override fun getToolWindowIcon(count: Int): Icon? {
    if (Experiments.getInstance().isFeatureEnabled("problems.view.project.errors.enabled")) return null
    val root = treeModel.root as? HighlightingFileRoot ?: return Toolwindows.ToolWindowProblemsEmpty
    val problem = root.getChildren(root.file).any {
      val severity = (it as? ProblemNode)?.severity
      severity != null && severity >= HighlightSeverity.ERROR.myVal
    }
    return if (problem) Toolwindows.ToolWindowProblems else Toolwindows.ToolWindowProblemsEmpty
  }

  override fun selectionChangedTo(selected: Boolean) {
    super.selectionChangedTo(selected)
    if (selected) updateCurrentFile()
  }

  fun selectHighlighter(highlighter: RangeHighlighterEx) {
    val root = treeModel.root as? HighlightingFileRoot
    root?.findProblemNode(highlighter)?.let { select(it) }
  }

  override fun powerSaveStateChanged() {
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
    powerSaveStateChanged()
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
    val status = getCurrentStatus()
    if (previousStatus != status) {
      previousStatus = status
      tree.emptyText.text = status.title
      if (status.details.isNotEmpty()) tree.emptyText.appendLine(status.details)
    }
    if (status.request) statusUpdateAlarm.cancelAndRequest()
  }

  private fun getCurrentStatus(): Status {
    val root = treeModel.root as? HighlightingFileRoot
    val file = root?.file ?: return Status(ProblemsViewBundle.message("problems.view.highlighting.no.selected.file"))
    if (PowerSaveMode.isEnabled()) return Status(ProblemsViewBundle.message("problems.view.highlighting.power.save.mode"))
    val document = ProblemsView.getDocument(project, file) ?: return statusAnalyzing(file)
    val editor = EditorFactory.getInstance().editors(document, project).findFirst().orElse(null) ?: return statusAnalyzing(file)
    val model = editor.markupModel as? EditorMarkupModel ?: return statusAnalyzing(file)
    val status = model.errorStripeRenderer?.getStatus(editor) ?: return statusAnalyzing(file)
    return when (status.analyzingType) {
      AnalyzingType.SUSPENDED -> Status(status.title, status.details, request = true)
      AnalyzingType.COMPLETE -> statusComplete(file, state.hideBySeverity.isNotEmpty())
      AnalyzingType.PARTIAL -> statusAnalyzing(file, state.hideBySeverity.isNotEmpty())
      else -> statusAnalyzing(file)
    }
  }

  private fun statusAnalyzing(file: VirtualFile, filtered: Boolean = false): Status {
    val title = ProblemsViewBundle.message("problems.view.highlighting.problems.analyzing", file.name)
    if (filtered) {
      val details = ProblemsViewBundle.message("problems.view.highlighting.problems.not.found.filter")
      return Status(title, details, request = true)
    }
    return Status(title, request = true)
  }

  private fun statusComplete(file: VirtualFile, filtered: Boolean): Status {
    val title = ProblemsViewBundle.message("problems.view.highlighting.problems.not.found", file.name)
    if (filtered) {
      val details = ProblemsViewBundle.message("problems.view.highlighting.problems.not.found.filter")
      return Status(title, details)
    }
    return Status(title)
  }
}

private data class Status(val title: String, val details: String = "", val request: Boolean = false)
