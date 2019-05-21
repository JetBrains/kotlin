// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints.presentation

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import java.awt.Point
import java.awt.event.MouseEvent
import javax.swing.SwingUtilities

/**
 * It is expected to be top level presentation, which have all the context to fill context menu
 */
class MenuOnClickPresentation(
  presentation: InlayPresentation,
  val project: Project,
  val actionsSupplier: () -> List<AnAction>
) : StaticDelegatePresentation(presentation) {
  override fun mouseClicked(e: MouseEvent, editorPoint: Point) {
    super.mouseClicked(e, editorPoint)
    if (SwingUtilities.isRightMouseButton(e) && !SwingUtilities.isLeftMouseButton(e)) {
      val manager = project.getComponent(ActionManager::class.java)
      val actions = actionsSupplier()
      if (actions.isEmpty()) return
      val popupMenu = manager.createActionPopupMenu("InlayMenu", DefaultActionGroup(actions))
      popupMenu.component.show(e.component, editorPoint.x, editorPoint.y)
    }
  }
}