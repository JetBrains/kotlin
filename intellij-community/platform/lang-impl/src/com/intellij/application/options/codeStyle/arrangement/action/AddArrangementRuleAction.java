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
import com.intellij.application.options.codeStyle.arrangement.match.EmptyArrangementRuleComponent;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationBundle;
import com.intellij.openapi.project.DumbAware;
import com.intellij.util.IconUtil;
import gnu.trove.TIntArrayList;
import org.jetbrains.annotations.NotNull;

/**
 * @author Denis Zhdanov
 */
public class AddArrangementRuleAction extends AbstractArrangementRuleAction implements DumbAware {
  
  public AddArrangementRuleAction() {
    getTemplatePresentation().setText(ApplicationBundle.message("arrangement.action.rule.add.text"));
    getTemplatePresentation().setDescription(ApplicationBundle.message("arrangement.action.rule.add.description"));
    getTemplatePresentation().setIcon(IconUtil.getAddIcon());
    setEnabledInModalContext(true);
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    ArrangementMatchingRulesControl control = getRulesControl(e);
    e.getPresentation().setEnabled(control != null);
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    ArrangementMatchingRulesControl control = getRulesControl(e);
    if (control == null) {
      return;
    }
    
    control.hideEditor();
    TIntArrayList rows = control.getSelectedModelRows();
    ArrangementMatchingRulesModel model = control.getModel();
    int rowToEdit;
    if (rows.size() == 1) {
      rowToEdit = rows.get(0) + 1;
      model.insertRow(rowToEdit, new Object[] {createNewRule(control)});
    }
    else {
      rowToEdit = model.getSize();
      model.add(createNewRule(control));
    }
    showEditor(control, rowToEdit);
    control.getSelectionModel().setSelectionInterval(rowToEdit, rowToEdit);
    scrollRowToVisible(control, rowToEdit);
  }

  @NotNull
  protected Object createNewRule(@NotNull ArrangementMatchingRulesControl control) {
    return new EmptyArrangementRuleComponent(control.getEmptyRowHeight());
  }

  protected void showEditor(@NotNull ArrangementMatchingRulesControl control, int rowToEdit) {
    control.showEditor(rowToEdit);
  }
}
