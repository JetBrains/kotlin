// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints

import com.intellij.codeInsight.CodeInsightBundle
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.actionSystem.Toggleable

class ToggleInlayHintsGloballyAction : ToggleAction(CodeInsightBundle.message("inlay.hints.toggle.action")), Toggleable {
  override fun isSelected(e: AnActionEvent): Boolean {
    return InlayHintsSettings.instance().hintsEnabledGlobally()
  }

  override fun setSelected(e: AnActionEvent, state: Boolean) {
    InlayHintsSettings.instance().setEnabledGlobally(state)
  }
}