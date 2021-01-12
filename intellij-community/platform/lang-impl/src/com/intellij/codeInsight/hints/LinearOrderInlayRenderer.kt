// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints

import com.intellij.codeInsight.hints.InlayHintsUtils.produceUpdatedRootList
import com.intellij.codeInsight.hints.presentation.InlayPresentation
import com.intellij.codeInsight.hints.presentation.PresentationListener
import com.intellij.codeInsight.hints.presentation.withTranslated
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.util.SmartList
import org.jetbrains.annotations.TestOnly
import java.awt.*
import java.awt.event.MouseEvent

/**
 * Renderer, that holds inside ordered list of [ConstrainedPresentation].
 * Invariant: holds at least one presentation
 */
abstract class LinearOrderInlayRenderer<Constraint : Any>(
  constrainedPresentations: Collection<ConstrainedPresentation<*, Constraint>>,
  private val createPresentation: (List<ConstrainedPresentation<*, Constraint>>) -> InlayPresentation,
  private val comparator: (ConstrainedPresentation<*, Constraint>) -> Int
) : PresentationContainerRenderer<Constraint> {
  // Supposed to be changed rarely and rarely contains more than 1 element
  private var presentations: List<ConstrainedPresentation<*, Constraint>> = SmartList(constrainedPresentations.sortedBy(comparator))

  init {
    assert(presentations.isNotEmpty())
  }

  private var cachedPresentation = createPresentation(presentations)

  private var _listener: PresentationListener? = null

  override fun addOrUpdate(new: List<ConstrainedPresentation<*, Constraint>>, editor: Editor, factory: InlayPresentationFactory) {
    assert(new.isNotEmpty())
    updateSorted(new.sortedBy(comparator), editor, factory)
  }

  override fun setListener(listener: PresentationListener) {
    val oldListener = _listener
    if (oldListener != null) {
      cachedPresentation.removeListener(oldListener)
    }
    _listener = listener
    cachedPresentation.addListener(listener)
  }

  private fun updateSorted(sorted: List<ConstrainedPresentation<*, Constraint>>,
                           editor: Editor,
                           factory: InlayPresentationFactory) {
    // TODO [roman.ivanov] here can be handled 1 old to 1 new situation without complex algorithms and allocations
    val tmp = produceUpdatedRootList(sorted, presentations, editor, factory)
    val oldSize = dimension()
    presentations = tmp
    _listener?.let {
      cachedPresentation.removeListener(it)
    }
    cachedPresentation = createPresentation(presentations)
    _listener?.let {
      cachedPresentation.addListener(it)
    }
    val newSize = dimension()
    if (oldSize != newSize) {
      cachedPresentation.fireSizeChanged(oldSize, newSize)
    }
    cachedPresentation.fireContentChanged(Rectangle(newSize))
  }

  private fun dimension() = Dimension(cachedPresentation.width, cachedPresentation.height)


  override fun paint(inlay: Inlay<*>, g: Graphics, targetRegion: Rectangle, textAttributes: TextAttributes) {
    g as Graphics2D
    g.withTranslated(targetRegion.x, targetRegion.y) {
      cachedPresentation.paint(g, effectsIn(textAttributes))
    }
  }

  override fun calcWidthInPixels(inlay: Inlay<*>): Int {
    return cachedPresentation.width
  }

  override fun calcHeightInPixels(inlay: Inlay<*>): Int {
    return cachedPresentation.height
  }

  // this should not be shown anywhere
  override fun getContextMenuGroupId(inlay: Inlay<*>): String {
    return "DummyActionGroup"
  }

  override fun mouseClicked(event: MouseEvent, translated: Point) {
    cachedPresentation.mouseClicked(event, translated)
  }

  override fun mouseMoved(event: MouseEvent, translated: Point) {
    cachedPresentation.mouseMoved(event, translated)
  }

  override fun mouseExited() {
    cachedPresentation.mouseExited()
  }

  override fun toString(): String {
    return cachedPresentation.toString()
  }

  @TestOnly
  fun getConstrainedPresentations(): List<ConstrainedPresentation<*, Constraint>> = presentations

  companion object {
    fun effectsIn(attributes: TextAttributes): TextAttributes {
      val result = TextAttributes()
      result.effectType = attributes.effectType
      result.effectColor = attributes.effectColor
      return result
    }
  }
}