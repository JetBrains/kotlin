/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package com.intellij.openapi.editor.actions;

import com.intellij.execution.impl.ConsoleViewUtil;
import com.intellij.find.EditorSearchSession;
import com.intellij.find.FindManager;
import com.intellij.find.FindModel;
import com.intellij.find.FindUtil;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorAction;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import org.jetbrains.annotations.NotNull;

public class IncrementalFindAction extends EditorAction {
  public static final Key<Boolean> SEARCH_DISABLED = Key.create("EDITOR_SEARCH_DISABLED");

  public static class Handler extends EditorActionHandler {

    private final boolean myReplace;

    public Handler(boolean isReplace) {

      myReplace = isReplace;
    }

    @Override
    public void execute(@NotNull final Editor editor, DataContext dataContext) {
      final Project project = CommonDataKeys.PROJECT.getData(DataManager.getInstance().getDataContext(editor.getComponent()));
      if (!editor.isOneLineMode()) {
        EditorSearchSession search = EditorSearchSession.get(editor);
        if (search != null) {
          search.getComponent().requestFocusInTheSearchFieldAndSelectContent(project);
          FindUtil.configureFindModel(myReplace, editor, search.getFindModel(), false);
        } else {
          FindManager findManager = FindManager.getInstance(project);
          FindModel model;
          if (myReplace) {
            model = findManager.createReplaceInFileModel();
          } else {
            model = new FindModel();
            model.copyFrom(findManager.getFindInFileModel());
          }
          boolean consoleViewEditor = ConsoleViewUtil.isConsoleViewEditor(editor);
          FindUtil.configureFindModel(myReplace, editor, model, consoleViewEditor);
          EditorSearchSession.start(editor, model, project).getComponent()
            .requestFocusInTheSearchFieldAndSelectContent(project);
          if (!consoleViewEditor && editor.getSelectionModel().hasSelection()) {
            // selection is used as string to find without search model modification so save the pattern explicitly
            FindUtil.updateFindInFileModel(project, model, true);
          }
        }
      }
    }

    @Override
    public boolean isEnabled(Editor editor, DataContext dataContext) {
      if (myReplace && ConsoleViewUtil.isConsoleViewEditor(editor) &&
          !ConsoleViewUtil.isReplaceActionEnabledForConsoleViewEditor(editor)) {
        return false;
      }
      if (SEARCH_DISABLED.get(editor, false)) {
        return false;
      }
      Project project = CommonDataKeys.PROJECT.getData(DataManager.getInstance().getDataContext(editor.getComponent()));
      return project != null && !editor.isOneLineMode();
    }
  }

  public IncrementalFindAction() {
    super(new Handler(false));
  }
}