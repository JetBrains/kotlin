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

package com.intellij.refactoring.ui;

import com.intellij.openapi.wm.IdeFocusManager;
import org.jetbrains.annotations.NonNls;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

/**
 * @author dsl
 */
public abstract class EnableDisableAction extends AbstractAction {
  @Override
  public void actionPerformed(ActionEvent e) {
    if (getTable().isEditing()) return;
    int[] rows = getTable().getSelectedRows();
    if (rows.length > 0) {
      boolean valueToBeSet = false;
      for (final int row : rows) {
        if (!isRowChecked(row)) {
          valueToBeSet = true;
          break;
        }
      }
      applyValue(rows, valueToBeSet);
//          myMyTableModel.fireTableRowsUpdated(rows[0], rows[rows.length - 1]);
    }
    IdeFocusManager.getGlobalInstance().doWhenFocusSettlesDown(() -> IdeFocusManager.getGlobalInstance().requestFocus(getTable(), true));
  }

  protected abstract JTable getTable();

  protected abstract void applyValue(int[] rows, boolean valueToBeSet);

  protected abstract boolean isRowChecked(int row);

  public void register() {// make SPACE check/uncheck selected rows
    JTable table = getTable();
    @NonNls InputMap inputMap = table.getInputMap();
    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "enable_disable");
    @NonNls final ActionMap actionMap = table.getActionMap();
    actionMap.put("enable_disable", this);
  }
}
