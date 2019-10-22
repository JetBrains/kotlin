// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints

import com.intellij.codeHighlighting.EditorBoundHighlightingPass
import com.intellij.codeInsight.daemon.impl.analysis.HighlightingLevelManager
import com.intellij.concurrency.JobLauncher
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.psi.PsiElement
import com.intellij.psi.SyntaxTraverser
import com.intellij.util.Processor


class InlayHintsPass(
  val rootElement: PsiElement,
  val collectors: List<CollectorWithSettings<out Any>>,
  editor: Editor,
  val settings: InlayHintsSettings
) : EditorBoundHighlightingPass(editor, rootElement.containingFile, true) {
  override fun doCollectInformation(progress: ProgressIndicator) {
    if (!HighlightingLevelManager.getInstance(myFile.project).shouldHighlight(myFile)) return
    JobLauncher.getInstance().invokeConcurrentlyUnderProgress(
      collectors,
      progress,
      true,
      false,
      Processor { collector ->
        val traverser = SyntaxTraverser.psiTraverser(rootElement)
        if (settings.hintsEnabled(collector.key, collector.language)) {
          for (element in traverser.preOrderDfsTraversal()) {
            if (!collector.collectHints(element, myEditor)) break
          }
        }
        true
      }
    )
  }

  override fun doApplyInformationToEditor() {
    val element = rootElement
    val startOffset = element.textOffset
    val endOffset = element.textRange.endOffset
    val inlayModel = myEditor.inlayModel
    // Marked those inlays, that were managed by some provider
    val existingHorizontalInlays: MarkList<Inlay<*>> = MarkList(inlayModel.getInlineElementsInRange(startOffset, endOffset))
    val existingVerticalInlays: MarkList<Inlay<*>> = MarkList(inlayModel.getBlockElementsInRange(startOffset, endOffset))
    for (collector in collectors) {
      collector.applyToEditor(
        myEditor,
        existingHorizontalInlays,
        existingVerticalInlays,
        settings.hintsEnabled(collector.key, collector.language)
      )
    }
    disposeOrphanInlays(existingHorizontalInlays)
    disposeOrphanInlays(existingVerticalInlays)
  }

  private fun disposeOrphanInlays(inlays: MarkList<Inlay<*>>) {
    // This may happen e. g. when extension of file is changed and providers that created inlay now not manage these inlays
    inlays.iterateNonMarked { _, inlay ->
      if (InlayHintsSinkImpl.isProvidersInlay(inlay)) {
        inlay.dispose()
      }
    }
  }
}
