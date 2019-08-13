/*
 * Copyright 2000-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.util.ui;

import com.intellij.codeInsight.lookup.LookupEx;
import com.intellij.codeInsight.lookup.LookupManager;
import com.intellij.ide.ui.UISettings;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CustomShortcutSet;
import com.intellij.openapi.editor.impl.EditorComponentImpl;
import com.intellij.ui.EditorTextField;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * @author Konstantin Bulenkov
 */
public class UpDownHandler {
  private static final CustomShortcutSet UP_KEY = CustomShortcutSet.fromString("UP");
  private static final CustomShortcutSet DOWN_KEY = CustomShortcutSet.fromString("DOWN");

  private UpDownHandler() {
  }

  public static void register(JComponent input, final JComponent affectedComponent) {
    register(input, affectedComponent, true);
  }
  
  public static void register(final JComponent input, final JComponent affectedComponent, boolean registerOnBothComponents) {
    final SelectionMover mover = new SelectionMover(affectedComponent);
    final AnAction up = new UpDownAction(mover, input, true);
    up.registerCustomShortcutSet(UP_KEY, input);

    final AnAction down = new UpDownAction(mover, input, false);
    down.registerCustomShortcutSet(DOWN_KEY, input);
    if (registerOnBothComponents) {
      up.registerCustomShortcutSet(UP_KEY, affectedComponent);
      down.registerCustomShortcutSet(DOWN_KEY, affectedComponent);
    }
  }

  private static class SelectionMover {
    private JComboBox myCombo;
    private JList myList;

    private SelectionMover(JComponent comp) {
      if (comp instanceof JComboBox) {
        myCombo = (JComboBox)comp;
      }
      else if (comp instanceof JList) {
        myList = (JList)comp;
      }
    }

    void move(int direction) {
      int index = -1;
      int size = 0;

      if (myCombo != null) {
        index = myCombo.getSelectedIndex();
        size = myCombo.getModel().getSize();
      } else if (myList != null) {
        index = myList.getSelectedIndex();
        size = myList.getModel().getSize();
      }

      if (index == -1 || size == 0) return;

      index += direction;

      if (index == size) {
        if (!UISettings.getInstance().getCycleScrolling()) return;
        index = 0;
      } else if (index == -1) {
        if (!UISettings.getInstance().getCycleScrolling()) return;
        index = size - 1;
      }

      if (myCombo != null) {
        myCombo.setSelectedIndex(index);
      } else if (myList != null) {
        myList.setSelectedIndex(index);
      }
    }
  }
  
  static class UpDownAction extends AnAction {
    private final int myDirection;
    private final SelectionMover myMover;
    private final JComponent myInput;

    UpDownAction(SelectionMover mover, JComponent input, boolean isUp) {
      super(isUp ? "Up" : "Down");
      myMover = mover;
      myInput = input;
      myDirection = isUp ? -1 : 1;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
      myMover.move(myDirection);
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
      final LookupEx lookup;
      if (myInput instanceof EditorTextField) {
        lookup = LookupManager.getActiveLookup(((EditorTextField)myInput).getEditor());
      } else if (myInput instanceof EditorComponentImpl) {
        lookup = LookupManager.getActiveLookup(((EditorComponentImpl)myInput).getEditor());
      } else {
        lookup = null;
      }

      JComboBox comboBox = UIUtil.findComponentOfType(myInput, JComboBox.class);
      boolean popupMenuVisible = comboBox != null && comboBox.isPopupVisible();

      e.getPresentation().setEnabled(lookup == null && !popupMenuVisible);
    }
  }
}
