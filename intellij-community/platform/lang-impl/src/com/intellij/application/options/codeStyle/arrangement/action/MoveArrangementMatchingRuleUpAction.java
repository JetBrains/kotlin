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
import com.intellij.openapi.application.ApplicationBundle;
import com.intellij.util.IconUtil;
import gnu.trove.TIntArrayList;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author Denis Zhdanov
 */
public class MoveArrangementMatchingRuleUpAction extends AbstractMoveArrangementRuleAction {

  public MoveArrangementMatchingRuleUpAction() {
    getTemplatePresentation().setText(ApplicationBundle.message("arrangement.action.rule.move.up.text"));
    getTemplatePresentation().setDescription(ApplicationBundle.message("arrangement.action.rule.move.up.description"));
    getTemplatePresentation().setIcon(IconUtil.getMoveUpIcon());
    setEnabledInModalContext(true);
  }

  @Override
  protected void fillMappings(@NotNull ArrangementMatchingRulesControl control, @NotNull List<int[]> mappings) {
    TIntArrayList rows = control.getSelectedModelRows();
    rows.reverse();
    int top = -1;
    for (int i = 0; i < rows.size(); i++) {
      int row = rows.get(i);
      if (row == top + 1) {
        mappings.add(new int[] { row, row });
        top++;
      }
      else {
        mappings.add(new int[]{ row, row - 1 });
      }
    } 
  }
}
