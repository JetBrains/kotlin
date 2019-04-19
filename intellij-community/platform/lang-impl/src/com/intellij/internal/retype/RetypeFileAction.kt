// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.internal.retype

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.ide.structureView.StructureViewTreeElement
import com.intellij.ide.structureView.impl.common.PsiTreeElementBase
import com.intellij.ide.util.treeView.smartTree.TreeElement
import com.intellij.internal.performance.latencyMap
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import java.util.*

/**
 * @author yole
 */
class RetypeFileAction : AnAction() {
  override fun actionPerformed(e: AnActionEvent) {
    val editor = e.getData(CommonDataKeys.EDITOR) as? EditorImpl
    val project = e.getData(CommonDataKeys.PROJECT) ?: return
    val existingSession = editor?.getUserData(RETYPE_SESSION_KEY)
    if (existingSession != null) {
      existingSession.stop(false)
    }
    else {
      val retypeOptions = RetypeOptions(project)
      val retypeOptionsDialog = RetypeOptionsDialog(project, retypeOptions, editor)
      if (!retypeOptionsDialog.showAndGet()) return
      val scriptBuilder = if (retypeOptions.recordScript) StringBuilder() else null
      val largeIndexFileCount = if (retypeOptions.enableLargeIndexing) retypeOptions.largeIndexFilesCount else -1
      latencyMap.clear()
      if (retypeOptions.retypeCurrentFile) {
        val session = RetypeSession(project, editor!!, retypeOptions.retypeDelay, scriptBuilder, retypeOptions.threadDumpDelay,
                                    restoreText = retypeOptions.restoreOriginalText,
                                    filesForIndexCount = largeIndexFileCount)
        session.start()
      }
      else {
        val queue = RetypeQueue(project, retypeOptions.retypeDelay, retypeOptions.threadDumpDelay, scriptBuilder,
                                largeIndexFileCount, retypeOptions.restoreOriginalText)
        if (!collectSizeSampledFiles(project,
                                     retypeOptions.retypeExtension.removePrefix("."),
                                     retypeOptions.fileCount,
                                     queue)) return
        queue.processNext()
      }
    }
  }

  data class CandidateFile(val virtualFile: VirtualFile, val size: Long)

  private fun collectSizeSampledFiles(project: Project, extension: String, count: Int, queue: RetypeQueue): Boolean {
    val candidates = mutableListOf<CandidateFile>()
    val result = ProgressManager.getInstance().runProcessWithProgressSynchronously(
      {
        ProjectRootManager.getInstance(project).fileIndex.iterateContent { file ->
          ProgressManager.checkCanceled()
          if (file.extension == extension && file.length > 0) {
            candidates.add(CandidateFile(file, file.length))
          }
          true
        }
      }, "Scanning files", true, project)
    if (!result) return false

    candidates.sortBy { it.size }
    if (count == 1) {
      queue.files.add(candidates[Random().nextInt(candidates.size)].virtualFile)
    }
    else {
      val stride = candidates.size / (count - 1)
      for (index in 0..candidates.size step stride) {
        queue.files.add(candidates[index.coerceAtMost(candidates.size - 1)].virtualFile)
      }
    }
    return true
  }

  override fun update(e: AnActionEvent) {
    val editor = e.getData(CommonDataKeys.EDITOR)
    e.presentation.isEnabled = e.project != null
    val retypeSession = editor?.getUserData(RETYPE_SESSION_KEY)
    if (retypeSession != null) {
      e.presentation.text = "Stop Retyping"
    }
    else {
      e.presentation.text = "Retype File(s)"
    }
  }
}

interface RetypeFileAssistant {
  fun acceptLookupElement(element: LookupElement): Boolean
  fun retypeDone(editor: Editor) {
  }

  companion object {
    val EP_NAME = ExtensionPointName.create<RetypeFileAssistant>("com.intellij.retypeFileAssistant")
  }
}

private class RetypeQueue(private val project: Project,
                          private val retypeDelay: Int,
                          private val threadDumpDelay: Int,
                          private val scriptBuilder: StringBuilder?,
                          private val largeIndexFileCount: Int,
                          private val restoreText: Boolean
) {
  val files = mutableListOf<VirtualFile>()
  private val threadDumps = mutableListOf<String>()

  fun processNext() {
    if (files.isEmpty()) return
    val file = files[0]
    files.removeAt(0)

    val editor = FileEditorManager.getInstance(project).openTextEditor(OpenFileDescriptor(project, file, 0), true) as EditorImpl
    selectFragmentToRetype(editor)
    val retypeSession = RetypeSession(project, editor, retypeDelay, scriptBuilder, threadDumpDelay, threadDumps,
                                      restoreText = restoreText,
                                      filesForIndexCount = largeIndexFileCount)
    if (files.isNotEmpty()) {
      retypeSession.startNextCallback = {
        ApplicationManager.getApplication().invokeLater { processNext() }
      }
    }
    retypeSession.start()
  }

  private fun selectFragmentToRetype(editor: Editor) {
    if (editor.document.textLength < 2000) return  // file is small, retype it all
    val fileEditor = FileEditorManager.getInstance(project).selectedEditor ?: return
    val structureView = fileEditor.structureViewBuilder?.createStructureView(fileEditor, project) ?: return
    try {
      val root = structureView.treeModel.root as? PsiTreeElementBase<*> ?: return
      val range = findRangeOfSuitableElement(root) ?: return
      editor.selectionModel.setSelection(range.startOffset, range.endOffset)
      editor.caretModel.moveToOffset(range.startOffset)
    }
    finally {
      Disposer.dispose(structureView)
    }
  }

  private fun findRangeOfSuitableElement(treeElement: TreeElement): TextRange? {
    for (child in treeElement.children) {
      val childRange = ((child as? StructureViewTreeElement)?.value as? PsiElement)?.textRange
      if (childRange != null) {
        if (childRange.length in 1000..2000) {
          return childRange
        }
        if (childRange.length > 2000) {
          val grandchildRange = findRangeOfSuitableElement(child)
          if (grandchildRange != null) {
            return grandchildRange
          }
        }
      }
    }
    return null
  }
}
