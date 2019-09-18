// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.ui.IdeUICustomization

/**
 * @author yole
 */
class IdeDependentActionGroup : DefaultActionGroup() {
  private val id by lazy { ActionManager.getInstance().getId(this) }

  override fun update(e: AnActionEvent) {
    super.update(e)
    val actionText = IdeUICustomization.getInstance().getActionText(id)
    if (actionText != null) {
      e.presentation.text = actionText
    }
  }

  override fun isDumbAware(): Boolean {
    return true
  }
}
