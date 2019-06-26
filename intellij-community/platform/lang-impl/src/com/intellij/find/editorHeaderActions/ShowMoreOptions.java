// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.find.editorHeaderActions;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.impl.ActionButton;
import com.intellij.openapi.actionSystem.impl.ActionToolbarImpl;
import com.intellij.openapi.project.DumbAware;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

public class ShowMoreOptions extends AnAction implements DumbAware {
  /**
   * @deprecated unused, use configurable shortcut {@code ShowFilterPopup}
   */
  @Deprecated
  public static final Shortcut SHORT_CUT = new KeyboardShortcut(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.ALT_DOWN_MASK | InputEvent.CTRL_DOWN_MASK), null);

  private final ActionToolbarImpl myToolbarComponent;

  //placeholder for keymap
  public ShowMoreOptions() {
    myToolbarComponent = null;
  }

  public ShowMoreOptions(ActionToolbarImpl toolbarComponent, JComponent shortcutHolder) {
    this.myToolbarComponent = toolbarComponent;
    KeyboardShortcut keyboardShortcut = ActionManager.getInstance().getKeyboardShortcut("ShowFilterPopup");
    if (keyboardShortcut != null) {
      registerCustomShortcutSet(new CustomShortcutSet(keyboardShortcut), shortcutHolder);
    }
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    final ActionButton secondaryActions = myToolbarComponent.getSecondaryActionsButton();
    if (secondaryActions != null) {
      secondaryActions.click();
    }
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    e.getPresentation().setEnabled(myToolbarComponent != null && myToolbarComponent.getSecondaryActionsButton() != null);
  }
}
