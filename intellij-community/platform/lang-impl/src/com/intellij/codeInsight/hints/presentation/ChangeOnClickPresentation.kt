// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints.presentation

import java.awt.Point
import java.awt.event.MouseEvent

/**
 * Presentation, that changes once it is first clicked
 */
class ChangeOnClickPresentation(
  private val notClicked: InlayPresentation,
  val onClick: () -> InlayPresentation
) : StatefulPresentation<ChangeOnClickPresentation.State>(State(false), ourMark) {
  private var cached : InlayPresentation? = null

  private fun getClickedPresentation(): InlayPresentation = when (val cachedVal = cached) {
    null -> {
      val presentation = onClick()
      cached = presentation
      presentation
    }
    else -> cachedVal
  }

  override fun getPresentation(): InlayPresentation = when {
    state.clicked -> getClickedPresentation()
    else -> notClicked
  }

  override fun toString(): String = when {
    state.clicked -> "<clicked>"
    else -> ""
  } + currentPresentation.toString()

  data class State(val clicked: Boolean)

  override fun mouseClicked(event: MouseEvent, translated: Point) {
    if (state.clicked) {
      super.mouseClicked(event, translated)
    } else {
      state = State(true)
    }
  }

  companion object {
    val ourMark = StateMark<State>("ChangeOnClick")
  }
}