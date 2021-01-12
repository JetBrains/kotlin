// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.application.options.codeStyle.arrangement.match.tokens;

import com.intellij.application.options.codeStyle.arrangement.color.ArrangementColorsProvider;
import com.intellij.openapi.application.ApplicationBundle;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.psi.codeStyle.arrangement.std.ArrangementStandardSettingsManager;
import com.intellij.psi.codeStyle.arrangement.std.StdArrangementRuleAliasToken;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
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

    final List<StdArrangementRuleAliasToken> tokenList = new ArrayList<>(tokens);
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
