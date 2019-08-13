// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints

import com.intellij.codeHighlighting.EditorBoundHighlightingPass
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.psi.PsiElement
import com.intellij.psi.SyntaxTraverser

class InlayHintsPass(
  val rootElement: PsiElement,
  val collectors: List<CollectorWithSettings<out Any>>,
  editor: Editor,
  val settings: InlayHintsSettings
) : EditorBoundHighlightingPass(editor, rootElement.containingFile, true) {
  val traverser = SyntaxTraverser.psiTraverser(rootElement)

  override fun doCollectInformation(progress: ProgressIndicator) {
    traverser.forEach { element ->
      for (collector in collectors) {
        if (settings.hintsEnabled(collector.key, collector.language)) {
          collector.collectHints(element, myEditor)
        }
      }
    }
  }

  override fun doApplyInformationToEditor() {
    val element = rootElement
    val startOffset = element.textOffset
    val endOffset = element.textRange.endOffset
    val inlayModel = myEditor.inlayModel
    val existingHorizontalInlays = inlayModel.getInlineElementsInRange(startOffset, endOffset)
    val existingVerticalInlays = inlayModel.getBlockElementsInRange(startOffset, endOffset)
    for (collector in collectors) {
      collector.applyToEditor(
        myEditor,
        existingHorizontalInlays,
        existingVerticalInlays,
        settings.hintsEnabled(collector.key, collector.language)
      )
    }
  }
}
