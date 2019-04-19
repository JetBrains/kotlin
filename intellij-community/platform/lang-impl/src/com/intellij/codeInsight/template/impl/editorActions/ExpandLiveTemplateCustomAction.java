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

import com.intellij.codeInsight.template.TemplateManager;
import com.intellij.codeInsight.template.impl.TemplateManagerImpl;
import com.intellij.codeInsight.template.impl.TemplateSettings;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorAction;
import com.intellij.openapi.editor.actionSystem.EditorWriteActionHandler;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author peter
 */
public class ExpandLiveTemplateCustomAction extends EditorAction {
  public ExpandLiveTemplateCustomAction() {
    super(createExpandTemplateHandler(TemplateSettings.CUSTOM_CHAR));
    setInjectedContext(true);
  }

  public static EditorWriteActionHandler createExpandTemplateHandler(final char shortcutChar) {
    return new EditorWriteActionHandler(true) {
      @Override
      public void executeWriteAction(Editor editor, @Nullable Caret caret, DataContext dataContext) {
        Project project = editor.getProject();
        assert project != null;
        TemplateManager.getInstance(project).startTemplate(editor, shortcutChar);
      }

      @Override
      protected boolean isEnabledForCaret(@NotNull Editor editor, @NotNull Caret caret, DataContext dataContext) {
        Project project = editor.getProject();
        return project != null &&
               ((TemplateManagerImpl)TemplateManager.getInstance(project)).prepareTemplate(editor, shortcutChar, null) != null;
      }
    };
  }
}
