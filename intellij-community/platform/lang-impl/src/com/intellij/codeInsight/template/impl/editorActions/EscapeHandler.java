/*
 * Copyright 2000-2016 JetBrains s.r.o.
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

package com.intellij.codeInsight.template.impl.editorActions;

import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.codeInsight.lookup.LookupManager;
import com.intellij.codeInsight.lookup.impl.LookupImpl;
import com.intellij.codeInsight.template.impl.TemplateManagerImpl;
import com.intellij.codeInsight.template.impl.TemplateState;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import org.jetbrains.annotations.NotNull;

public class EscapeHandler extends EditorActionHandler {
  private final EditorActionHandler myOriginalHandler;

  public EscapeHandler(EditorActionHandler originalHandler) {
    myOriginalHandler = originalHandler;
  }

  @Override
  public void execute(@NotNull Editor editor, DataContext dataContext) {
    TemplateState templateState = TemplateManagerImpl.getTemplateState(editor);
    if (templateState != null && !templateState.isFinished()) {
      SelectionModel selectionModel = editor.getSelectionModel();
      LookupImpl lookup = (LookupImpl)LookupManager.getActiveLookup(editor);

      // the idea behind lookup checking is that if there is a preselected value in lookup 
      // then user might want just to close lookup but not finish a template.
      // E.g. user wants to move to the next template segment by Tab without completion invocation.
      // If there is no selected value in completion that user definitely wants to finish template
      boolean lookupIsEmpty = lookup == null || lookup.getCurrentItem() == null;
      if (!selectionModel.hasSelection() && lookupIsEmpty) {
        CommandProcessor.getInstance().setCurrentCommandName(CodeInsightBundle.message("finish.template.command"));
        templateState.gotoEnd(true);
        return;
      }
    }

    if (myOriginalHandler.isEnabled(editor, dataContext)) {
      myOriginalHandler.execute(editor, dataContext);
    }
  }

  @Override
  public boolean isEnabled(Editor editor, DataContext dataContext) {
    final TemplateState templateState = TemplateManagerImpl.getTemplateState(editor);
    if (templateState != null && !templateState.isFinished()) {
      return true;
    }
    return myOriginalHandler.isEnabled(editor, dataContext);
  }
}
