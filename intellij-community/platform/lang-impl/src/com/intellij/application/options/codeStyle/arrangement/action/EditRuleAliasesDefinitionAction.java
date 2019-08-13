// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.application.options.codeStyle.arrangement.action;

import com.intellij.application.options.codeStyle.arrangement.match.ArrangementSectionRulesControl;
import com.intellij.application.options.codeStyle.arrangement.match.tokens.ArrangementRuleAliasDialog;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationBundle;
import org.jetbrains.annotations.NotNull;

/**
 * @author Svetlana.Zemlyanskaya
 */
public class EditRuleAliasesDefinitionAction extends AnAction {

  public EditRuleAliasesDefinitionAction() {
    getTemplatePresentation().setText(ApplicationBundle.message("arrangement.action.alias.rule.add.edit.text"));
    getTemplatePresentation().setDescription(ApplicationBundle.message("arrangement.action.alias.rule.add.edit.description"));
    getTemplatePresentation().setIcon(AllIcons.General.GearPlain);
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    final ArrangementSectionRulesControl control = e.getData(ArrangementSectionRulesControl.KEY);
    if (control == null) {
      return;
    }
    e.getPresentation().setEnabledAndVisible(control.getRulesAliases() != null);
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    ArrangementSectionRulesControl control = e.getData(ArrangementSectionRulesControl.KEY);
    if (control == null || control.getRulesAliases() == null) {
      return;
    }

    control.hideEditor();
    final ArrangementRuleAliasDialog dialog = control.createRuleAliasEditDialog();
    if (dialog.showAndGet() && dialog.isModified()) {
      control.setRulesAliases(dialog.getRuleAliases());
    }
  }
}
