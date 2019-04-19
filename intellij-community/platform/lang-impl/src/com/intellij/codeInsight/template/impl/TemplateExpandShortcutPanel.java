// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.template.impl;

import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.actionSystem.Shortcut;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.keymap.impl.ui.KeymapPanel;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.options.ex.Settings;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.HyperlinkAdapter;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.ListCellRendererWrapper;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class TemplateExpandShortcutPanel extends JPanel {
  private final JComboBox myExpandByCombo;
  private final HyperlinkLabel myOpenKeymapLabel;

  private static final String SPACE = CodeInsightBundle.message("template.shortcut.space");
  private static final String TAB = CodeInsightBundle.message("template.shortcut.tab");
  private static final String ENTER = CodeInsightBundle.message("template.shortcut.enter");
  private static final String CUSTOM = CodeInsightBundle.message("template.shortcut.custom");

  public TemplateExpandShortcutPanel(@NotNull String label) {
    super(new GridBagLayout());
    GridBagConstraints gbConstraints = new GridBagConstraints();
    gbConstraints.weighty = 0;
    gbConstraints.weightx = 0;
    gbConstraints.gridy = 0;
    add(new JLabel(label), gbConstraints);

    gbConstraints.gridx = 1;
    gbConstraints.insets = new Insets(0, 4, 0, 0);
    myExpandByCombo = new ComboBox();
    add(myExpandByCombo, gbConstraints);

    myOpenKeymapLabel = new HyperlinkLabel("Change");
    gbConstraints.gridx = 2;
    add(myOpenKeymapLabel, gbConstraints);

    gbConstraints.gridx = 3;
    gbConstraints.weightx = 1;
    add(new JPanel(), gbConstraints);
    setBorder(new EmptyBorder(0, 0, 10, 0));

    myExpandByCombo.addItemListener(new ItemListener() {
      @Override
      public void itemStateChanged(ItemEvent e) {
        myOpenKeymapLabel.setVisible(myExpandByCombo.getSelectedItem() == CUSTOM);
      }
    });
    for (String s : ContainerUtil.ar(SPACE, TAB, ENTER, CUSTOM)) {
      //noinspection unchecked
      myExpandByCombo.addItem(s);
    }
    //noinspection unchecked
    myExpandByCombo.setRenderer(new ListCellRendererWrapper() {
      @Override
      public void customize(JList list, Object value, int index, boolean selected, boolean hasFocus) {
        if (value == CUSTOM) {
          Shortcut[] shortcuts = getCurrentCustomShortcuts();
          String shortcutText = shortcuts.length == 0 ? "" : KeymapUtil.getShortcutsText(shortcuts);
          setText(StringUtil.isEmpty(shortcutText) ? "Custom..." : "Custom (" + shortcutText + ")");
        }
      }

      private Shortcut[] getCurrentCustomShortcuts() {
        Settings allSettings = Settings.KEY.getData(DataManager.getInstance().getDataContext(myOpenKeymapLabel));
        KeymapPanel keymapPanel = allSettings == null ? null : allSettings.find(KeymapPanel.class);
        Shortcut[] shortcuts = keymapPanel == null ? null : keymapPanel.getCurrentShortcuts(IdeActions.ACTION_EXPAND_LIVE_TEMPLATE_CUSTOM);
        if (shortcuts == null) {
          Shortcut shortcut = ActionManager.getInstance().getKeyboardShortcut(IdeActions.ACTION_EXPAND_LIVE_TEMPLATE_CUSTOM);
          shortcuts = shortcut == null ? Shortcut.EMPTY_ARRAY : new Shortcut[]{shortcut};
        }
        return shortcuts;
      }
    });
    addPropertyChangeListener(new PropertyChangeListener() {
      @Override
      public void propertyChange(final PropertyChangeEvent evt) {
        if (isConfigurableOpenEvent(evt)) {
          resizeComboToFitCustomShortcut();
        }
      }

      private boolean isConfigurableOpenEvent(PropertyChangeEvent evt) {
        return evt.getPropertyName().equals("ancestor") && evt.getNewValue() != null && evt.getOldValue() == null;
      }
    });

    myOpenKeymapLabel.addHyperlinkListener(new HyperlinkAdapter() {
      @Override
      protected void hyperlinkActivated(HyperlinkEvent e) {
        Settings allSettings = Settings.KEY.getData(DataManager.getInstance().getDataContext(myOpenKeymapLabel));
        final KeymapPanel keymapPanel = allSettings == null ? new KeymapPanel() : allSettings.find(KeymapPanel.class);
        if (keymapPanel == null) return;

        Runnable selectAction = () -> keymapPanel.selectAction(IdeActions.ACTION_EXPAND_LIVE_TEMPLATE_CUSTOM);
        if (allSettings != null) {
          allSettings.select(keymapPanel).doWhenDone(selectAction);
        }
        else {
          ShowSettingsUtil.getInstance().editConfigurable(myOpenKeymapLabel, keymapPanel, selectAction);
          resizeComboToFitCustomShortcut();
        }
      }
    });
  }

  public String getSelectedString() {
    return (String)myExpandByCombo.getSelectedItem();
  }

  public void setSelectedChar(char ch) {
    myExpandByCombo.setSelectedItem(ch == TemplateSettings.CUSTOM_CHAR ? CUSTOM :
                                    ch == TemplateSettings.TAB_CHAR ? TAB :
                                    ch == TemplateSettings.ENTER_CHAR ? ENTER :
                                    SPACE);
  }

  public char getSelectedChar() {
    Object selectedItem = myExpandByCombo.getSelectedItem();
    if (TAB.equals(selectedItem)) return TemplateSettings.TAB_CHAR;
    if (ENTER.equals(selectedItem)) return TemplateSettings.ENTER_CHAR;
    if (SPACE.equals(selectedItem)) {
      return TemplateSettings.SPACE_CHAR;
    }
    else {
      return TemplateSettings.CUSTOM_CHAR;
    }
  }

  @SuppressWarnings("unchecked")
  private void resizeComboToFitCustomShortcut() {
    myExpandByCombo.setPrototypeDisplayValue(null);
    myExpandByCombo.setPrototypeDisplayValue(CUSTOM);
    myExpandByCombo.revalidate();
    myExpandByCombo.repaint();
  }
}
