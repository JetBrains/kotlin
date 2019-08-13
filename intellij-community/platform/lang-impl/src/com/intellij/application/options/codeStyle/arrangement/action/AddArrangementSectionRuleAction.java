/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
import com.intellij.application.options.codeStyle.arrangement.match.ArrangementSectionRuleManager;
import com.intellij.application.options.codeStyle.arrangement.match.ArrangementSectionRulesControl;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationBundle;
import org.jetbrains.annotations.NotNull;

/**
 * @author Svetlana.Zemlyanskaya
 */
public class AddArrangementSectionRuleAction extends AddArrangementRuleAction {

  public AddArrangementSectionRuleAction() {
    getTemplatePresentation().setText(ApplicationBundle.message("arrangement.action.section.rule.add.text"));
    getTemplatePresentation().setDescription(ApplicationBundle.message("arrangement.action.section.rule.add.description"));
    getTemplatePresentation().setIcon(AllIcons.CodeStyle.AddNewSectionRule);
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    final ArrangementMatchingRulesControl control = getRulesControl(e);
    if (!(control instanceof ArrangementSectionRulesControl)) {
      e.getPresentation().setEnabledAndVisible(false);
      return;
    }
    e.getPresentation().setEnabledAndVisible(((ArrangementSectionRulesControl)control).getSectionRuleManager() != null);
  }

  @NotNull
  @Override
  protected Object createNewRule(@NotNull ArrangementMatchingRulesControl control) {
    final ArrangementSectionRuleManager manager = ((ArrangementSectionRulesControl)control).getSectionRuleManager();
    assert manager != null;
    return manager.createDefaultSectionRule();
  }

  @Override
  protected void showEditor(@NotNull ArrangementMatchingRulesControl control, int rowToEdit) {
    final ArrangementSectionRuleManager manager = ((ArrangementSectionRulesControl)control).getSectionRuleManager();
    if (manager != null) {
      manager.showEditor(rowToEdit);
    }
  }
}
