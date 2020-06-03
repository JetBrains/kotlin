// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints.presentation

/**
 * Presentation, that may be in two states and can preserve state type between passes.
 */
open class BiStatePresentation(
  private val first: () -> InlayPresentation,
  private val second: () -> InlayPresentation,
  initiallyFirstEnabled: Boolean
) : StatefulPresentation<BiStatePresentation.State>(State(initiallyFirstEnabled), STATE_MARK) {
  override fun getPresentation(): InlayPresentation {
    return when (state.currentFirst) {
      true -> first()
      else -> second()
    }
  }

  fun flipState() {
    state = State(!state.currentFirst)
  }

  fun setFirst() {
    state = State(true)
  }

  fun setSecond() {
    state = State(false)
  }

  override fun toString(): String = currentPresentation.toString()

  data class State(val currentFirst: Boolean)

  companion object {
    @JvmStatic
    val STATE_MARK = StateMark<State>("BiState")
  }
}