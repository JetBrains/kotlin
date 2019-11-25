// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.intention.impl.preview

import com.intellij.codeInsight.daemon.impl.ShowIntentionsPass
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInsight.intention.impl.CachedIntentions
import com.intellij.codeInsight.intention.impl.IntentionActionWithTextCaching
import com.intellij.codeInsight.intention.impl.ShowIntentionActionsHandler
import com.intellij.diff.comparison.ComparisonManager
import com.intellij.diff.comparison.ComparisonPolicy
import com.intellij.diff.fragments.LineFragment
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.progress.DumbProgressIndicator
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Computable
import com.intellij.openapi.util.ThrowableComputable
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile

class IntentionPreviewComputable(private val project: Project,
                                 private val action: IntentionAction,
                                 private val originalFile: PsiFile,
                                 private val originalEditor: Editor,
                                 private val psiFileCopy: PsiFile,
                                 private val editorCopy: Editor
                                 ) : Computable<Pair<PsiFile?, List<LineFragment>>?> {
  override fun compute(): Pair<PsiFile?, List<LineFragment>>? {
    CommandProcessor.getInstance().runUndoTransparentAction(Runnable {
      try {
        ApplicationManager.getApplication().runReadAction(ThrowableComputable<Unit, Exception> {
          val actionWithTextCachingCopy = intentionActionWithTextCaching(editorCopy, psiFileCopy)
          if (actionWithTextCachingCopy == null) return@ThrowableComputable

          try {
            originalEditor.document.setReadOnly(true)
            val fileEditorPair = ShowIntentionActionsHandler.chooseFileForAction(psiFileCopy, editorCopy, actionWithTextCachingCopy.action)
                                 ?: return@ThrowableComputable
            ProgressManager.checkCanceled()
            actionWithTextCachingCopy.action.invoke(project, fileEditorPair.second, fileEditorPair.first)
            ProgressManager.checkCanceled()
          }
          finally {
            originalEditor.document.setReadOnly(false)
            ApplicationManager.getApplication().invokeLater(Runnable { EditorFactory.getInstance().releaseEditor(editorCopy) })
          }
        })
      }
      catch (e: Exception) {
        LOG.warn("There are exceptions on invocation the intention: '${action.text}' on a copy of the file.")
        throw ProcessCanceledException()
      }
    })

    return Pair<PsiFile?, List<LineFragment>>(
      psiFileCopy,
      PsiDocumentManager.getInstance(project).commitAndRunReadAction(Computable {
        ComparisonManager.getInstance().compareLines(originalFile.text, psiFileCopy.text, ComparisonPolicy.TRIM_WHITESPACES,
                                                     DumbProgressIndicator.INSTANCE)
      }))
  }

  private fun intentionActionWithTextCaching(editorCopy: Editor,
                                             psiFileCopy: PsiFile): IntentionActionWithTextCaching? {
    val actionsToShow = ShowIntentionsPass.getActionsToShow(editorCopy, psiFileCopy, false)
    val cachedIntentions = CachedIntentions.createAndUpdateActions(project, psiFileCopy, editorCopy, actionsToShow)
    return getFixes(cachedIntentions).find { it.text == action.text }
  }


  fun getFixes(cachedIntentions: CachedIntentions): Sequence<IntentionActionWithTextCaching> =
    sequenceOf<IntentionActionWithTextCaching>()
      .plus(cachedIntentions.intentions)
      .plus(cachedIntentions.inspectionFixes)
      .plus(cachedIntentions.errorFixes)

  companion object {
    private val LOG = Logger.getInstance(IntentionPreviewComputable::class.java)
  }
}