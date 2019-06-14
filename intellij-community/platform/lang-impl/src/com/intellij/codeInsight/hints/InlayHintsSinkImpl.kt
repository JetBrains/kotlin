// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints

import com.intellij.codeInsight.hints.presentation.InlayPresentation
import com.intellij.codeInsight.hints.presentation.PresentationListener
import com.intellij.codeInsight.hints.presentation.PresentationRenderer
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorCustomElementRenderer
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.InlayModel
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.util.DocumentUtil
import gnu.trove.TIntObjectHashMap
import java.awt.Dimension
import java.awt.Rectangle


private sealed class InlayHint(val offset: Int, val presentation: InlayPresentation)

private class InlineElement(
  offset: Int,
  val relatesToPrecedingText: Boolean,
  presentation: InlayPresentation
) : InlayHint(offset, presentation)

private class BlockElement(
  offset: Int,
  val relatesToPrecedingText: Boolean,
  val showAbove: Boolean,
  val priority: Int,
  presentation: InlayPresentation
) : InlayHint(offset, presentation)

private class HintsAtOffset(var inlineElement: InlineElement?, var blockElement: BlockElement?)

class InlayHintsSinkImpl<T>(val key: SettingsKey<T>) : InlayHintsSink {
  private val hints = TIntObjectHashMap<HintsAtOffset>()

  override fun addInlineElement(offset: Int, relatesToPrecedingText: Boolean, presentation: InlayPresentation) {
    addHint(InlineElement(offset, relatesToPrecedingText, presentation))
  }

  override fun addBlockElement(offset: Int,
                               relatesToPrecedingText: Boolean,
                               showAbove: Boolean,
                               priority: Int,
                               presentation: InlayPresentation) {
    addHint(BlockElement(offset, relatesToPrecedingText, showAbove, priority, presentation))
  }

  private fun addHint(hint: InlayHint) {
    var hintsAtOffset = hints[hint.offset]
    if (hintsAtOffset == null) {
      hintsAtOffset = HintsAtOffset(null, null)
      hints.put(hint.offset, hintsAtOffset)
    }
    when (hint) {
      is InlineElement -> {
        if (hintsAtOffset.inlineElement == null) {
          hintsAtOffset.inlineElement = hint
        } else {
          logAtTheSameOffset(hint)
        }
      }
      is BlockElement -> {
        if (hintsAtOffset.blockElement == null) {
          hintsAtOffset.blockElement = hint
        } else {
          logAtTheSameOffset(hint)
        }
      }
    }
  }

  private fun logAtTheSameOffset(hint: InlayHint) {
    LOG.warn("Hint added to the same offset: ${hint.offset} ${hint.presentation}")
  }


  /**
   * This method called every time, when it is required to update hints even for disabled providers.
   */
  fun applyToEditor(editor: Editor,
                    existingHorizontalInlays: List<Inlay<EditorCustomElementRenderer>>,
                    existingVerticalInlays: List<Inlay<EditorCustomElementRenderer>>,
                    isEnabled: Boolean) {
    val inlayModel = editor.inlayModel
    val isBulkChange = existingHorizontalInlays.size + hints.size() > BulkChangeThreshold
    DocumentUtil.executeInBulk(editor.document, isBulkChange) {
      updateOrDeleteExistingHints(existingHorizontalInlays, existingVerticalInlays, isEnabled)
      createNewHints(inlayModel)
    }
    hints.clear()
  }

  private fun createNewHints(inlayModel: InlayModel) {
    hints.forEachEntry { offset, hints ->
      hints.inlineElement?.let {
        createNewHint(inlayModel, it, offset)
      }
      hints.blockElement?.let {
        createNewHint(inlayModel, it, offset)
      }
      true
    }
  }

  private fun createNewHint(inlayModel: InlayModel, hint: InlayHint, offset: Int) : Inlay<PresentationRenderer>? {
    val presentation = hint.presentation
    val presentationRenderer = PresentationRenderer(presentation)
    val inlay = when (hint) {
                  is InlineElement -> inlayModel.addInlineElement(offset, hint.relatesToPrecedingText, presentationRenderer)
                  is BlockElement -> inlayModel.addBlockElement(
                    offset,
                    hint.relatesToPrecedingText,
                    hint.showAbove,
                    hint.priority,
                    presentationRenderer
                  )
                } ?: return null
    inlay.putUserData(INLAY_KEY, key)
    presentation.addListener(InlayListener(inlay))
    return inlay
  }

  class InlayListener(private val inlay: Inlay<PresentationRenderer>) : PresentationListener {
    // TODO be more accurate during invalidation (requires changes in Inlay)
    override fun contentChanged(area: Rectangle) = inlay.repaint()

    override fun sizeChanged(previous: Dimension, current: Dimension) = inlay.updateSize()
  }

  private fun updateOrDeleteExistingHints(
    existingHorizontalInlays: List<Inlay<EditorCustomElementRenderer>>,
    existingVerticalInlays: List<Inlay<EditorCustomElementRenderer>>,
    isEnabled: Boolean
  ) {
    updateOrDeleteExistingHints(existingHorizontalInlays, true, isEnabled)
    updateOrDeleteExistingHints(existingVerticalInlays, false, isEnabled)
  }

  private fun updateOrDeleteExistingHints(existingInlays: List<Inlay<EditorCustomElementRenderer>>, isInline: Boolean, isEnabled: Boolean) {
    for (inlay in existingInlays) {
      val inlayKey = inlay.getUserData(INLAY_KEY) as SettingsKey<*>?
      if (inlayKey != key) continue
      val offset = inlay.offset
      val hint = when (val hintsAtOffset = hints[offset]) {
        null -> null
        else -> when {
          isInline -> hintsAtOffset.inlineElement
          else -> hintsAtOffset.blockElement
        }
      }
      if (hint == null || !isEnabled) {
        Disposer.dispose(inlay)
      }
      else {
        val newPresentation = hint.presentation
        val renderer = inlay.renderer as PresentationRenderer
        val previousPresentation = renderer.presentation
        @Suppress("UNCHECKED_CAST")
        newPresentation.addListener(InlayListener(inlay as Inlay<PresentationRenderer>))
        renderer.presentation = newPresentation
        if (newPresentation.updateState(previousPresentation)) {
          newPresentation.fireUpdateEvent(previousPresentation.dimension())
        }
        hints.remove(offset)
      }
    }
  }

  companion object {
    private val INLAY_KEY: Key<Any?> = Key.create("INLAY_KEY")
    private const val BulkChangeThreshold = 1000

    @JvmField val LOG = logger<InlayHintsSinkImpl<*>>()
  }
}