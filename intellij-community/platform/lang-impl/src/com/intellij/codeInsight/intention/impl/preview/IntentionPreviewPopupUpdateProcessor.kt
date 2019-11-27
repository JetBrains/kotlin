// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.intention.impl.preview

import com.intellij.codeInsight.CodeInsightBundle
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInsight.intention.impl.IntentionHintComponent
import com.intellij.codeInsight.intention.impl.preview.IntentionPreviewComponent.Companion.LOADING_PREVIEW
import com.intellij.codeInsight.intention.impl.preview.IntentionPreviewComponent.Companion.NO_PREVIEW
import com.intellij.diff.fragments.LineFragment
import com.intellij.openapi.actionSystem.CommonShortcuts.ESCAPE
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.keymap.KeymapUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.psi.PsiFile
import com.intellij.ui.popup.PopupPositionManager
import com.intellij.ui.popup.PopupUpdateProcessor
import com.intellij.util.Alarm
import com.intellij.util.concurrency.AppExecutorUtil
import org.jetbrains.concurrency.CancellablePromise
import java.util.concurrent.TimeUnit

internal class IntentionPreviewPopupUpdateProcessor(private val project: Project,
                                                    private val originalFile: PsiFile,
                                                    private val originalEditor: Editor) : PopupUpdateProcessor(project) {
  private var index: Int = LOADING_PREVIEW
  private var show = false
  private val alarm = Alarm()

  private lateinit var popup: JBPopup
  private lateinit var component: IntentionPreviewComponent
  private lateinit var updateAdvText: (String) -> Unit

  private var editorsToRelease = mutableListOf<EditorEx>()

  override fun updatePopup(intentionAction: Any?) {
    if (!show) return

    alarm.cancelAllRequests()
    if (!::popup.isInitialized || popup.isDisposed) {
      component = IntentionPreviewComponent(project)
      component.multiPanel.select(LOADING_PREVIEW, true)

      popup = JBPopupFactory.getInstance().createComponentPopupBuilder(component, null)
        .setCancelCallback { cancel() }
        .createPopup()

      PopupPositionManager.positionPopupInBestPosition(popup, originalEditor, null)

      updateAdvText.invoke(CodeInsightBundle.message("intention.preview.adv.hide.text", Companion.ESCAPE_SHORTCUT_TEXT))
    }

    val value = component.multiPanel.getValue(index, false)
    if (value != null) {
      select(index)
      return
    }

    val action = intentionAction as IntentionAction
    if (!action.startInWriteAction() || action.getElementToMakeWritable(originalFile) !== originalFile) {
      select(NO_PREVIEW)
      return
    }

    UpdatePopup(project, action, originalFile, originalEditor).start()
  }

  fun setup(updateAdvConsumer: (String) -> Unit, parentIndex: Int) {
    index = parentIndex
    updateAdvText = updateAdvConsumer
  }

  private fun cancel(): Boolean {
    editorsToRelease.forEach { EditorFactory.getInstance().releaseEditor(it) }
    editorsToRelease.clear()
    component.removeAll()
    alarm.cancelAllRequests()
    show = false
    updateAdvText.invoke(
      CodeInsightBundle.message("intention.preview.adv.show.text", IntentionHintComponent.INTENTION_PREVIEW_SHORTCUT_TEXT))
    return true
  }

  inner class UpdatePopup(private val project: Project,
                          private val action: IntentionAction,
                          private val originalFile: PsiFile,
                          private val originalEditor: Editor) : Runnable {
    lateinit var computation: CancellablePromise<Pair<PsiFile?, List<LineFragment>>?>

    fun start() {
      component.startLoading()

      computation = ReadAction.nonBlocking<Pair<PsiFile?, List<LineFragment>>>(
        IntentionPreviewComputable(project, action, originalFile, originalEditor)).submit(AppExecutorUtil.getAppExecutorService())

      alarm.addRequest(this, 100)
    }

    override fun run() {
      if (!computation.isCancelled && !computation.isDone) {
        alarm.addRequest(this, 200)
        return
      }

      try {
        val editors = IntentionPreviewModel.createEditors(project, originalFile, computation.get(3, TimeUnit.SECONDS))
        if (editors.isEmpty()) {
          select(NO_PREVIEW)
          return
        }

        editorsToRelease.addAll(editors)
        select(index, editors)
      }
      catch (e: Exception) {
        select(NO_PREVIEW)
      }
    }
  }

  fun toggleShow() {
    show = !show
  }

  private fun select(index: Int, editors: List<EditorEx> = emptyList()) {
    component.stopLoading()
    component.editors = editors
    component.multiPanel.select(index, true)

    popup.pack(true, true)
  }

  companion object {
    private val ESCAPE_SHORTCUT_TEXT = KeymapUtil.getPreferredShortcutText(ESCAPE.shortcuts)
  }
}