package com.intellij.find.editorHeaderActions;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.CustomShortcutSet;
import com.intellij.openapi.actionSystem.Shortcut;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.PopupChooserBuilder;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.util.List;

public class Utils {
  private Utils() {
  }

  public static void showCompletionPopup(JComponent toolbarComponent,
                                         final JList list,
                                         String title,
                                         final JTextComponent textField,
                                         String ad) {

    final Runnable callback = () -> {
      String selectedValue = (String)list.getSelectedValue();
      if (selectedValue != null) {
        textField.setText(selectedValue);
        IdeFocusManager.getGlobalInstance().requestFocus(textField, false);
      }
    };

    final PopupChooserBuilder builder = JBPopupFactory.getInstance().createListPopupBuilder(list);
    if (title != null) {
      builder.setTitle(title);
    }
    final JBPopup popup = builder.setMovable(false).setResizable(false)
      .setRequestFocus(true).setItemChoosenCallback(callback).createPopup();

    if (ad != null) {
      popup.setAdText(ad, SwingConstants.LEFT);
    }

    if (toolbarComponent != null) {
      popup.showUnderneathOf(toolbarComponent);
    }
    else {
      popup.showUnderneathOf(textField);
    }
  }

  public static void setSmallerFont(final JComponent component) {
    if (SystemInfo.isMac) {
      component.setFont(JBUI.Fonts.smallFont());
    }
  }

  public static void setSmallerFontForChildren(JComponent component) {
    for (Component c : component.getComponents()) {
      if (c instanceof JComponent) {
        setSmallerFont((JComponent)c);
      }
    }
  }

  @NotNull
  public static CustomShortcutSet shortcutSetOf(@NotNull List<Shortcut> shortcuts) {
    return new CustomShortcutSet(shortcuts.toArray(Shortcut.EMPTY_ARRAY));
  }

  @NotNull
  public static List<Shortcut> shortcutsOf(@NotNull String actionId) {
    AnAction action = ActionManager.getInstance().getAction(actionId);
    return action == null ? ContainerUtil.emptyList() : ContainerUtil.immutableList(action.getShortcutSet().getShortcuts());
  }
}
