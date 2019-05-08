// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.issue.quickfix

import com.intellij.codeInsight.highlighting.HighlightManager
import com.intellij.codeInsight.highlighting.HighlightUsagesHandler
import com.intellij.find.FindManager
import com.intellij.find.FindModel
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import org.jetbrains.annotations.ApiStatus
import com.intellij.build.issue.BuildIssueQuickFix
import org.jetbrains.plugins.gradle.util.GradleUtil

import java.util.concurrent.CompletableFuture

import java.util.concurrent.CompletableFuture.completedFuture

/**
 * @author Vladislav.Soroka
 */
@ApiStatus.Experimental
class GradleWrapperSettingsOpenQuickFix(private val myProjectPath: String, private val mySearch: String?) : BuildIssueQuickFix {

  override val id: String = "open_gradle_wrapper_settings"

  override fun runQuickFix(project: Project): CompletableFuture<*> {
    showWrapperPropertiesFile(project, myProjectPath, mySearch)
    return completedFuture<Any>(null)
  }

  companion object {
    fun showWrapperPropertiesFile(project: Project, projectPath: String, search: String?) {
      val wrapperPropertiesFile = GradleUtil.findDefaultWrapperPropertiesFile(projectPath) ?: return
      ApplicationManager.getApplication().invokeLater {
        val file = VfsUtil.findFileByIoFile(wrapperPropertiesFile, false) ?: return@invokeLater
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
