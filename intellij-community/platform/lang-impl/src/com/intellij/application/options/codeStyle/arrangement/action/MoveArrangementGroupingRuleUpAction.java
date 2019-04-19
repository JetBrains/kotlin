// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.application.options.codeStyle.arrangement.action;

import com.intellij.application.options.codeStyle.arrangement.group.ArrangementGroupingRulesControl;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationBundle;
import com.intellij.openapi.project.DumbAware;
import com.intellij.util.IconUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.table.DefaultTableModel;

/**
 * @author Denis Zhdanov
 */
public class MoveArrangementGroupingRuleUpAction extends AnAction implements DumbAware {

  public MoveArrangementGroupingRuleUpAction() {
    getTemplatePresentation().setText(ApplicationBundle.message("arrangement.action.rule.move.up.text"));
    getTemplatePresentation().setDescription(ApplicationBundle.message("arrangement.action.rule.move.up.description"));
    getTemplatePresentation().setIcon(IconUtil.getMoveUpIcon());
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    ArrangementGroupingRulesControl control = e.getData(ArrangementGroupingRulesControl.KEY);
    if (control == null) {
      e.getPresentation().setEnabled(false);
      return;
    }

    int[] rows = control.getSelectedRows();
    e.getPresentation().setEnabled(rows.length == 1 && rows[0] != 0);
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    ArrangementGroupingRulesControl control = e.getData(ArrangementGroupingRulesControl.KEY);
    if (control == null) {
      return;
    }

    int[] rows = control.getSelectedRows();
    int row = rows[0];
    if (rows.length != 1 || row == 0) {
      return;
    }

    if (control.isEditing()) {
      control.getCellEditor().stopCellEditing();
    }

    DefaultTableModel model = control.getModel();
    Object value = model.getValueAt(row, 0);
    model.removeRow(row);
    model.insertRow(row - 1, new Object[] { value });
    control.getSelectionModel().setSelectionInterval(row - 1, row - 1);
  }
}
