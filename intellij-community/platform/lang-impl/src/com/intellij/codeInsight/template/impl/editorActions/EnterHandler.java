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
package com.intellij.codeInsight.template.impl.editorActions;

import com.intellij.codeInsight.editorActions.BaseEnterHandler;
import com.intellij.codeInsight.template.TemplateManager;
import com.intellij.codeInsight.template.impl.TemplateSettings;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class EnterHandler extends BaseEnterHandler {
  private final EditorActionHandler myOriginalHandler;

  public EnterHandler(EditorActionHandler originalHandler) {
    super(true);
    myOriginalHandler = originalHandler;
  }

  @Override
  public boolean isEnabledForCaret(@NotNull Editor editor, @NotNull Caret caret, DataContext dataContext) {
    return myOriginalHandler.isEnabled(editor, caret, dataContext);
  }

  @Override
  public void executeWriteAction(Editor editor, Caret caret, DataContext dataContext) {
    final Project project = editor.getProject();
    if (project != null && TemplateManager.getInstance(project).startTemplate(editor, TemplateSettings.ENTER_CHAR)) {
      return;
    }

    if (myOriginalHandler != null) {
      myOriginalHandler.execute(editor, caret, dataContext);
    }
  }
}
