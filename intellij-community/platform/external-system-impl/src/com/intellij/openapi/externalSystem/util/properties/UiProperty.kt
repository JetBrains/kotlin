// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.util.properties

import com.intellij.openapi.Disposable
import com.intellij.openapi.ui.ValidationInfo
import javax.swing.JComponent

interface UiProperty<T> : GraphProperty<T> {
  fun bind(component: JComponent, validate: () -> ValidationInfo?, parentDisposable: Disposable)

  fun validate(): ValidationInfo?
}