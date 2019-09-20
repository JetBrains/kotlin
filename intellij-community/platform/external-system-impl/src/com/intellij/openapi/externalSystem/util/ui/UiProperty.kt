// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.util.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.ui.ComponentValidator
import com.intellij.openapi.ui.ValidationInfo
import javax.swing.JComponent

class UiProperty<T>(initial: () -> T) : IdeProperty<T>(initial) {

  private var validator: ComponentValidator? = null

  fun bind(component: JComponent, validate: () -> ValidationInfo?, parentDisposable: Disposable) {
    validator = ComponentValidator(parentDisposable)
      .withValidator { -> validate()?.forComponent(component) }
      .installOn(component)
    addListener { validate() }
  }

  fun validate(): ValidationInfo? {
    val validator = validator ?: return null
    validator.revalidate()
    return validator.validationInfo
  }
}