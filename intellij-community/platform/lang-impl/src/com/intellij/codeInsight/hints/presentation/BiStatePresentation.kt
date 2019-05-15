// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints.presentation

import java.awt.Point
import java.awt.event.MouseEvent

class BiStatePresentation(
  val first: () -> InlayPresentation,
  val second: () -> InlayPresentation,
  initialState: Boolean
) : StatefulPresentation<BiStatePresentation.State>(State(initialState), STATE_MARK) {
  override fun getPresentation(): InlayPresentation {
    return when (state.first) {
      true -> first()
      else -> second()
    }
  }

  fun flipState() {
    state = State(!state.first)
  }

  override fun toString(): String = currentPresentation.toString()

  data class State(val first: Boolean)

  companion object {
    @JvmStatic
    val STATE_MARK = StateMark<State>("BiState")
  }
}