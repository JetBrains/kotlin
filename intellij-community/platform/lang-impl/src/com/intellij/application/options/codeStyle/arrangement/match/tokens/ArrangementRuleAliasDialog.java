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
package com.intellij.application.options.codeStyle.arrangement.match.tokens;

import com.intellij.application.options.codeStyle.arrangement.color.ArrangementColorsProvider;
import com.intellij.openapi.application.ApplicationBundle;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.psi.codeStyle.arrangement.std.StdArrangementRuleAliasToken;
import com.intellij.psi.codeStyle.arrangement.std.ArrangementStandardSettingsManager;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * @author Svetlana.Zemlyanskaya
 */
public class ArrangementRuleAliasDialog extends DialogWrapper {
  private final ArrangementRuleAliasesListEditor myEditor;
  private boolean myModified;

  public ArrangementRuleAliasDialog(@Nullable Project project,
                                    @NotNull ArrangementStandardSettingsManager settingsManager,
                                    @NotNull ArrangementColorsProvider colorsProvider,
                                    @NotNull Collection<StdArrangementRuleAliasToken> tokens,
                                    @NotNull Set<String> tokensInUse) {
    super(project, false);

    final List<StdArrangementRuleAliasToken> tokenList = ContainerUtil.newArrayList(tokens);
    myEditor = new ArrangementRuleAliasesListEditor(settingsManager, colorsProvider, tokenList, tokensInUse);
    if (!tokenList.isEmpty()) {
      myEditor.selectItem(tokenList.get(0));
    }

    setTitle(ApplicationBundle.message("arrangement.settings.section.rule.custom.token.title"));
    init();
  }

  public boolean isModified() {
    return myModified;
  }

  public Collection<StdArrangementRuleAliasToken> getRuleAliases() {
    return myEditor.getItems();
  }

  @Nullable
  @Override
  protected JComponent createCenterPanel() {
    return myEditor.createComponent();
  }

  @Override
  protected void doOKAction() {
    try {
      myModified = myEditor.isModified();
      if (myModified) {
        myEditor.apply();
      }
      super.doOKAction();
    }
    catch (ConfigurationException e) {
      // show error
    }
  }
}
