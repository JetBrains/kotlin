// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints.presentation

import java.awt.Point
import java.awt.event.MouseEvent

/**
 * Changes state once hovered, not switches state back
 * Preserves hover state between passes.
 * @param onHoverPredicate predicate, that considers state is hovered (additional to fact, that it is in bounds)
 */
class ChangeOnHoverPresentation(
  private val noHover: InlayPresentation,
  private val hover: () -> InlayPresentation,
  private val onHoverPredicate: (MouseEvent) -> Boolean = { true }
) : StatefulPresentation<ChangeOnHoverPresentation.State>(State(false), STATE_MARK) {
  override fun getPresentation(): InlayPresentation = when (state.isInside) {
    true -> hover()
    false -> noHover
  }

  override fun toString(): String {
    return when (state.isInside) {
      true -> "<hovered>"
      false -> ""
    } + currentPresentation.toString()
  }

  override fun mouseMoved(event: MouseEvent, translated: Point) {
    super.mouseMoved(event, translated)
    if (!onHoverPredicate(event)) {
      state = State(false)
      return
    }
    if (!state.isInside) {
      state = State(true)
    }
  }

  override fun mouseExited() {
    super.mouseExited()
    if (state.isInside) {
      state = State(false)
    }
  }

  data class State(val isInside: Boolean)

  companion object {
    @JvmStatic
    val STATE_MARK = StateMark<State>("OnHover")
  }
}