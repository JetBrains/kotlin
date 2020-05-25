// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions;

import com.intellij.codeWithMe.ClientId;
import com.intellij.ide.HelpTooltip;
import com.intellij.ide.IdeBundle;
import com.intellij.ide.actions.searcheverywhere.SearchEverywhereManagerImpl;
import com.intellij.ide.lightEdit.LightEdit;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.CustomComponentAction;
import com.intellij.openapi.actionSystem.impl.ActionButton;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.keymap.MacKeymapUtil;
import com.intellij.openapi.keymap.impl.ModifierKeyDoubleClickHandler;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.util.FontUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Konstantin Bulenkov
 */
public class SearchEverywhereAction extends SearchEverywhereBaseAction implements CustomComponentAction, DumbAware, DataProvider {

  public static final Key<ConcurrentHashMap<ClientId, JBPopup>> SEARCH_EVERYWHERE_POPUP = new Key<>("SearchEverywherePopup");

  static {
    ModifierKeyDoubleClickHandler.getInstance().registerAction(IdeActions.ACTION_SEARCH_EVERYWHERE, KeyEvent.VK_SHIFT, -1, false);
  }

  public SearchEverywhereAction() {
    setEnabledInModalContext(false);
  }

  @NotNull
  @Override
  public JComponent createCustomComponent(@NotNull Presentation presentation, @NotNull String place) {
    return new ActionButton(this, presentation, place, ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE) {
      @Override protected void updateToolTipText() {
        String shortcutText = getShortcut();

        String classesTabName = String.join("/",GotoClassPresentationUpdater.getActionTitlePluralized());
        if (Registry.is("ide.helptooltip.enabled")) {
          HelpTooltip.dispose(this);

          new HelpTooltip()
            .setTitle(myPresentation.getText())
            .setShortcut(shortcutText)
            .setDescription(IdeBundle.message("search.everywhere.action.tooltip.description.text", classesTabName))
            .installOn(this);
        }
        else {
          setToolTipText(IdeBundle.message("search.everywhere.action.tooltip.text", shortcutText, classesTabName));
        }
      }
    };
  }

  @Nullable
  @Override
  public Object getData(@NotNull @NonNls String dataId) {
    return null;
  }

  private static String getShortcut() {
    Shortcut[] shortcuts = KeymapUtil.getActiveKeymapShortcuts(IdeActions.ACTION_SEARCH_EVERYWHERE).getShortcuts();
    if (shortcuts.length == 0) {
      return "Double" + (SystemInfo.isMac ? FontUtil.thinSpace() + MacKeymapUtil.SHIFT : " Shift");
    }
    return KeymapUtil.getShortcutsText(shortcuts);
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    if (LightEdit.owns(e.getProject())) return;

    if (Registry.is("ide.suppress.double.click.handler") && e.getInputEvent() instanceof KeyEvent) {
      if (((KeyEvent)e.getInputEvent()).getKeyCode() == KeyEvent.VK_SHIFT) {
        return;
      }
    }

    showInSearchEverywherePopup(SearchEverywhereManagerImpl.ALL_CONTRIBUTORS_GROUP_ID, e, true, true);
  }
}

