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

package com.intellij.codeInsight.template.impl.actions;

import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.codeInsight.template.impl.TemplateManagerImpl;
import com.intellij.codeInsight.template.impl.TemplateState;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorAction;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NextVariableAction extends EditorAction {
  public NextVariableAction() {
    super(new Handler());
    setInjectedContext(true);
  }

  @Override
  public boolean startInTransaction() {
    return true;
  }

  private static class Handler extends EditorActionHandler {

    @Override
    protected void doExecute(@NotNull Editor editor, @Nullable Caret caret, DataContext dataContext) {
      TemplateState templateState = TemplateManagerImpl.getTemplateState(editor);
      assert templateState != null;
      CommandProcessor.getInstance().setCurrentCommandName(CodeInsightBundle.message("template.next.variable.command"));
      templateState.nextTab();
    }

    @Override
    protected boolean isEnabledForCaret(@NotNull Editor editor, @NotNull Caret caret, DataContext dataContext) {
      TemplateState templateState = TemplateManagerImpl.getTemplateState(editor);
      return templateState != null && !templateState.isFinished() && templateState.isToProcessTab();
    }
  }
}
