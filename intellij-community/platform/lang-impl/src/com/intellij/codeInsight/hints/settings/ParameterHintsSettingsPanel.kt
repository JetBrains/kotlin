// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints.settings

import com.intellij.codeInsight.hints.BlackListDialog
import com.intellij.lang.Language
import com.intellij.ui.components.labels.LinkLabel
import java.awt.Component
import javax.swing.*

class ParameterHintsSettingsPanel(val language: Language,
                                  blackListSupported: Boolean) : JPanel() {

  init {
    layout = BoxLayout(this, BoxLayout.Y_AXIS)
    if (blackListSupported) {
      val label = LinkLabel.create("Black list...") {
        BlackListDialog(language).show()
      }
      label.alignmentX = Component.LEFT_ALIGNMENT
      add(label)
    }
  }
}
