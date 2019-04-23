// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints

import com.intellij.codeHighlighting.EditorBoundHighlightingPass
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.psi.PsiElement
import com.intellij.psi.SyntaxTraverser

class InlayHintsPass(
  val rootElement: PsiElement,
  collectors: List<CollectorWithSettings<out Any>>,
  editor: Editor,
  val settings: InlayHintsSettings
) : EditorBoundHighlightingPass(editor, rootElement.containingFile, true) {
  val traverser = SyntaxTraverser.psiTraverser(rootElement)
  private val collectors = collectors.map { CollectorInfo(it, settings.hintsEnabled(it.key, it.language)) }

  override fun doCollectInformation(progress: ProgressIndicator) {
    traverser.forEach { element ->
      for ((collector, enabled) in collectors) {
        collector.collectHints(element, enabled, myEditor)
      }
    }
  }

  override fun doApplyInformationToEditor() {
    val element = rootElement
    val startOffset = element.textOffset
    val endOffset = element.textRange.endOffset
    val existingInlays = myEditor.inlayModel.getInlineElementsInRange(startOffset, endOffset)
    for ((collector, _) in collectors) {
      collector.applyToEditor(myFile, myEditor, existingInlays)
    }
  }


  private data class CollectorInfo(
    val collector: CollectorWithSettings<out Any>,
    val isEnabled: Boolean
  )
}
