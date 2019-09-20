// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.build.issue.quickfix

import com.intellij.build.issue.BuildIssueQuickFix
import com.intellij.codeInsight.highlighting.HighlightManager
import com.intellij.codeInsight.highlighting.HighlightUsagesHandler
import com.intellij.find.FindManager
import com.intellij.find.FindModel
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import org.jetbrains.annotations.ApiStatus
import java.nio.file.Path
import java.util.concurrent.CompletableFuture

/**
 * @author Vladislav.Soroka
 */
@ApiStatus.Experimental
class OpenFileQuickFix(private val path: Path, private val search: String?) : BuildIssueQuickFix {
  override val id: String
    get() = path.toString()

  override fun runQuickFix(project: Project, dataProvider: DataProvider): CompletableFuture<*> {
    val future = CompletableFuture<Any>()
    ApplicationManager.getApplication().invokeLater {
      try {
        showFile(project, path, search)
        future.complete(null)
      }
      catch (e: Exception) {
        future.completeExceptionally(e)
      }
    }
    return future
  }

  companion object {
    fun showFile(project: Project, path: Path, search: String?) {
      ApplicationManager.getApplication().invokeLater {
        val file = VfsUtil.findFileByIoFile(path.toFile(), false) ?: return@invokeLater
        val editor = FileEditorManager.getInstance(project).openTextEditor(OpenFileDescriptor(project, file), false)
        if (search == null || editor == null) return@invokeLater

        val attributes = EditorColorsManager.getInstance().globalScheme.getAttributes(EditorColors.SEARCH_RESULT_ATTRIBUTES)
        val findModel = FindModel().apply { FindModel.initStringToFind(this, search) }
        val findResult = FindManager.getInstance(project).findString(editor.document.charsSequence, 0, findModel, file)
        val highlightManager = HighlightManager.getInstance(project)
        HighlightUsagesHandler.highlightRanges(highlightManager, editor, attributes, false, listOf(findResult))
      }
    }
  }
}