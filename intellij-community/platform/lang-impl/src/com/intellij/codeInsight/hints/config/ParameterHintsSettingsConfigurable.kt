// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints.config

import com.intellij.codeInsight.hints.ChangeListener
import com.intellij.codeInsight.hints.ImmediateConfigurable
import com.intellij.codeInsight.hints.Option
import com.intellij.lang.Language
import javax.swing.JComponent

class ParameterHintsSettingsConfigurable(val options: List<Option>, language: Language, blackListSupported: Boolean) : ImmediateConfigurable {
  private val panel = ParameterHintsSettingsPanel(language, options, blackListSupported)

  override fun createComponent(listener: ChangeListener): JComponent {
    return panel
  }
}