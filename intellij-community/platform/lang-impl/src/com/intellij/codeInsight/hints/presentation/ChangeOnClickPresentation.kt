// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints.presentation

import java.awt.Point
import java.awt.event.MouseEvent

class ChangeOnClickPresentation(
  private val notClicked: InlayPresentation,
  val onClick: PresentationSupplier
) : StatefulPresentation<ChangeOnClickPresentation.State>(State(false), ourMark) {
  override fun getPresentation(): InlayPresentation = when {
    state.clicked -> onClick.getPresentation() // TODO cache it (may be called multiple times - and usually do called multiple times)
    else -> notClicked
  }

  override fun toString(): String = when {
    state.clicked -> "<clicked>"
    else -> ""
  } + currentPresentation.toString()

  data class State(val clicked: Boolean)

  override fun mouseClicked(e: MouseEvent, editorPoint: Point) {
    if (state.clicked) {
      super.mouseClicked(e, editorPoint)
    } else {
      state = State(true)
    }
  }

  companion object {
    val ourMark = StateMark<State>("ChangeOnClick")
  }
}

/**
 * Implementors of this interface must implement meaningful equals
 */
interface PresentationSupplier {
  fun getPresentation() : InlayPresentation
}