// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints.presentation

class SynchronousPresentation<S>(
  val presentation: StatefulPresentation<S>,
  val others: List<StatefulPresentation<S>>
) : StatefulPresentation<S>(
  presentation.state,
  presentation.stateMark
) {

  override var state: S
    get() = super.state
    set(value) {
      super.state = value
      for (other in others) {
        other.state = value
      }
    }

  override fun getPresentation(): InlayPresentation = presentation

  override fun toString(): String = presentation.toString()
}