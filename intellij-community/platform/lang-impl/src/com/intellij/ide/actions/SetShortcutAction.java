// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions;

import com.intellij.codeWithMe.ClientId;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.keymap.Keymap;
import com.intellij.openapi.keymap.KeymapManager;
import com.intellij.openapi.keymap.impl.ActionShortcutRestrictions;
import com.intellij.openapi.keymap.impl.ui.KeymapPanel;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.ui.popup.JBPopup;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.Optional;

public class SetShortcutAction extends AnAction implements DumbAware {

  public final static DataKey<AnAction> SELECTED_ACTION = DataKey.create("SelectedAction");

  public SetShortcutAction() {
    setEnabledInModalContext(true);
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    JBPopup seDialog = getPopup(e).orElse(null);
    if (seDialog == null) return;

    KeymapManager km = KeymapManager.getInstance();
    Keymap activeKeymap = km != null ? km.getActiveKeymap() : null;
    if (activeKeymap == null) {
      return;
    }

    AnAction action = e.getData(SELECTED_ACTION);
    Component component = e.getData(PlatformDataKeys.CONTEXT_COMPONENT);
    if (action == null || component == null) {
      return;
    }

    seDialog.cancel();
    String id = ActionManager.getInstance().getId(action);
    KeymapPanel.addKeyboardShortcut(id, ActionShortcutRestrictions.getInstance().getForActionId(id), activeKeymap, component);
  }

  @NotNull
  private static Optional<JBPopup> getPopup(@NotNull AnActionEvent e) {
    return Optional.ofNullable(e.getProject()).map(it -> it.getUserData(SearchEverywhereAction.SEARCH_EVERYWHERE_POPUP))
      .map(it -> it.get(ClientId.getCurrent()));
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    Presentation presentation = e.getPresentation();

    Optional<JBPopup> seDialog = getPopup(e);
    if (!seDialog.isPresent()) {
      presentation.setEnabled(false);
      return;
    }

    KeymapManager km = KeymapManager.getInstance();
    Keymap activeKeymap = km != null ? km.getActiveKeymap() : null;
    if (activeKeymap == null) {
      presentation.setEnabled(false);
      return;
    }

    AnAction action = e.getData(SELECTED_ACTION);
    Component component = e.getData(PlatformDataKeys.CONTEXT_COMPONENT);
    presentation.setEnabled(action != null && component != null);
  }
}
