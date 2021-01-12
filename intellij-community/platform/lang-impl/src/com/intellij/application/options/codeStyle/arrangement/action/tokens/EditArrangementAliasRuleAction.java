// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.application.options.codeStyle.arrangement.action.tokens;

import com.intellij.application.options.codeStyle.arrangement.action.EditArrangementRuleAction;
import com.intellij.application.options.codeStyle.arrangement.match.ArrangementMatchingRulesControl;
import com.intellij.application.options.codeStyle.arrangement.match.tokens.ArrangementRuleAliasControl;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Svetlana.Zemlyanskaya
 */
public class EditArrangementAliasRuleAction extends EditArrangementRuleAction {
  @Override
  @Nullable
  protected ArrangementMatchingRulesControl getRulesControl(@NotNull AnActionEvent e) {
    return e.getData(ArrangementRuleAliasControl.KEY);
  }
}
