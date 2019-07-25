// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints

import com.intellij.codeInsight.hints.presentation.InlayPresentation

/**
 * Component that represents stateful hint
 * @param S state of the component
 */
abstract class HintComponent<S : Any>(
  private var state: S
) {
  // TODO lazy presentation
  private var presentation : InlayPresentation = render(state)

  abstract fun render(s: S) : InlayPresentation

  // TODO just update? we may want to update hints's state partially
  open fun shouldUpdate(newState: S): Boolean {
    return state != newState
  }

  fun setState(newState: S) {
    state = newState
    presentation = render(state)
  }
}