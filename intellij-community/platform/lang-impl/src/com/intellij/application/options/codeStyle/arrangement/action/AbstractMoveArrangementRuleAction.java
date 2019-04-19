/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.intellij.application.options.codeStyle.arrangement.action;

import com.intellij.application.options.codeStyle.arrangement.match.ArrangementMatchingRulesControl;
import com.intellij.application.options.codeStyle.arrangement.match.ArrangementMatchingRulesModel;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAware;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Denis Zhdanov
 */
public abstract class AbstractMoveArrangementRuleAction extends AbstractArrangementRuleAction implements DumbAware {

  @Override
  public void update(@NotNull AnActionEvent e) {
    ArrangementMatchingRulesControl control = getRulesControl(e);
    if (control == null) {
      e.getPresentation().setEnabled(false);
      return;
    }

    final List<int[]> mappings = new ArrayList<>();
    fillMappings(control, mappings);
    for (int[] mapping : mappings) {
      if (mapping[0] != mapping[1]) {
        e.getPresentation().setEnabled(true);
        return;
      }
    }
    e.getPresentation().setEnabled(false);
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    final ArrangementMatchingRulesControl control = getRulesControl(e);
    if (control == null) {
      return;
    }

    final int editing = control.getEditingRow() - 1;

    control.runOperationIgnoreSelectionChange(() -> {
      control.hideEditor();
      final List<int[]> mappings = new ArrayList<>();
      fillMappings(control, mappings);

      if (mappings.isEmpty()) {
        return;
      }

      int newRowToEdit = editing;
      ArrangementMatchingRulesModel model = control.getModel();
      Object value;
      int from;
      int to;
      for (int[] pair : mappings) {
        from = pair[0];
        to = pair[1];
        if (from != to) {
          value = model.getElementAt(from);
          model.removeRow(from);
          model.insert(to, value);
          if (newRowToEdit == from) {
            newRowToEdit = to;
          }
        }
      }

      ListSelectionModel selectionModel = control.getSelectionModel();
      for (int[] pair : mappings) {
        selectionModel.addSelectionInterval(pair[1], pair[1]);
      }


      int visibleRow = -1;
      if (newRowToEdit >= 0) {
        control.showEditor(newRowToEdit);
        visibleRow = newRowToEdit;
      }
      else if (!mappings.isEmpty()) {
        visibleRow = mappings.get(0)[1];
      }

      if (visibleRow != -1) {
        scrollRowToVisible(control, visibleRow);
      }
    });
    control.repaintRows(0, control.getModel().getSize() - 1, true);

  }

  protected abstract void fillMappings(@NotNull ArrangementMatchingRulesControl control, @NotNull List<int[]> mappings);
}
