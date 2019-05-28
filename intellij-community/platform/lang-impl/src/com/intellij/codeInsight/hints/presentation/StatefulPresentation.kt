// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints.presentation

import com.intellij.codeInsight.hints.dimension
import com.intellij.codeInsight.hints.fireUpdateEvent
import com.intellij.openapi.editor.markup.TextAttributes
import java.awt.Dimension
import java.awt.Graphics2D
import java.awt.Point
import java.awt.Rectangle
import java.awt.event.MouseEvent

/**
 * Partially reimplementation of [DynamicDelegatePresentation], but with state
 * @param S state, immutable object, requires to implement meaningful equals
 */
abstract class StatefulPresentation<S: Any?>(
  state: S,
  val stateMark: StateMark<S>
) : BasePresentation() {
  private var _state = state
  open var state: S
  get() = _state
  set(value) {
    if (value != _state) {
      val previous = dimension()
      updateStateAndPresentation(value)
      fireUpdateEvent(previous)
    }
  }

  private fun updateStateAndPresentation(value: S) {
    val previous = dimension()
    _state = value
    val presentation = getPresentation()
    updatePresentation(presentation)
    fireUpdateEvent(previous)
  }

  private var _currentPresentation: InlayPresentation? = null

  val currentPresentation: InlayPresentation
  get() = when (val current = _currentPresentation) {
    null -> {
      val presentation = getPresentation()
      updatePresentation(presentation)
      presentation
    }
    else -> current
  }

  private fun updatePresentation(presentation: InlayPresentation) {
    _currentPresentation?.removeListener(listener)
    listener = DelegateListener()
    presentation.addListener(listener)
    _currentPresentation = presentation
  }

  private var listener = DelegateListener()

  override val width: Int
    get() = currentPresentation.width
  override val height: Int
    get() = currentPresentation.height

  override fun updateState(previousPresentation: InlayPresentation): Boolean {
    if (previousPresentation !is StatefulPresentation<*>) return true
    val previousState = previousPresentation._state
    var changed = false
    if (previousState != _state) {
      val previousMark = previousPresentation.stateMark
      if (stateMark == previousMark) {
        val castedPrevious = stateMark.cast(previousState, previousMark)
        if (castedPrevious != null) {
          updateStateAndPresentation(castedPrevious)
          changed = true
        }
      }
    }
    currentPresentation.updateState(previousPresentation.currentPresentation)
    return changed
  }

  /**
   * Method returns actual presentation, depending on state only.
   * Called once state is changed, presentation cached.
   * If you want to get actual presentation, use [currentPresentation]
   */
  abstract fun getPresentation() : InlayPresentation

  override fun paint(g: Graphics2D, attributes: TextAttributes) {
    currentPresentation.paint(g, attributes)
  }

  override fun mouseClicked(event: MouseEvent, translated: Point) {
    currentPresentation.mouseClicked(event, translated)
  }

  override fun mouseMoved(event: MouseEvent, translated: Point) {
    currentPresentation.mouseMoved(event, translated)
  }

  override fun mouseExited() {
    currentPresentation.mouseExited()
  }

  private inner class DelegateListener : PresentationListener {
    override fun contentChanged(area: Rectangle) = fireContentChanged(area)

    override fun sizeChanged(previous: Dimension, current: Dimension) = fireSizeChanged(previous, current)
  }

  /**
   * Used to provide type safe access to data
   * @param id must be different for any different types
   */
  data class StateMark<T: Any?>(val id: String) {
    @Suppress("UNCHECKED_CAST")
    fun cast(value: Any?, otherMark: StateMark<*>): T? {
      if (this != otherMark) return null
      return value as T?
    }
  }
}