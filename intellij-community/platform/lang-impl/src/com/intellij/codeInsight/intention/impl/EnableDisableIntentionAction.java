// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.intention.impl;

import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInsight.intention.impl.config.IntentionManagerSettings;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;

public class EnableDisableIntentionAction extends AbstractEditIntentionSettingsAction {
  private final IntentionAction myAction;

  public EnableDisableIntentionAction(@NotNull IntentionAction action) {
    super(action);
    myAction = action;
  }

  @Override
  @NotNull
  public String getText() {
    final IntentionManagerSettings mySettings = IntentionManagerSettings.getInstance();
    return CodeInsightBundle.message(mySettings.isEnabled(myAction) ? "disable.intention.action" : "enable.intention.action", myFamilyName);
  }

  @Override
  public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
    final IntentionManagerSettings mySettings = IntentionManagerSettings.getInstance();
    mySettings.setEnabled(myAction, !mySettings.isEnabled(myAction));
  }

  @Override
  public String toString() {
    return getText();
  }
}
