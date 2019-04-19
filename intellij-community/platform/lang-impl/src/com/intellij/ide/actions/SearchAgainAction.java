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

package com.intellij.ide.actions;

import com.intellij.find.FindManager;
import com.intellij.find.FindUtil;
import com.intellij.ide.IdeBundle;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.fileEditor.ex.IdeDocumentHistory;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import org.jetbrains.annotations.NotNull;

import static com.intellij.openapi.editor.actions.IncrementalFindAction.SEARCH_DISABLED;

public class SearchAgainAction extends AnAction implements DumbAware {
  public SearchAgainAction() {
    setEnabledInModalContext(true);
  }

  @Override
  public void actionPerformed(@NotNull final AnActionEvent e) {
    final Project project = e.getData(CommonDataKeys.PROJECT);
    final FileEditor editor = e.getData(PlatformDataKeys.FILE_EDITOR);
    if (editor == null || project == null) return;
    CommandProcessor commandProcessor = CommandProcessor.getInstance();
    commandProcessor.executeCommand(
      project, () -> {
        PsiDocumentManager.getInstance(project).commitAllDocuments();
        IdeDocumentHistory.getInstance(project).includeCurrentCommandAsNavigation();
        FindManager findManager = FindManager.getInstance(project);
        if(!findManager.selectNextOccurrenceWasPerformed() && findManager.findNextUsageInEditor(editor)) {
          return;
        }

        FindUtil.searchAgain(project, editor, e.getDataContext());
      },
      IdeBundle.message("command.find.next"),
      null
    );
  }

  @Override
  public void update(@NotNull AnActionEvent event){
    Presentation presentation = event.getPresentation();
    Project project = event.getData(CommonDataKeys.PROJECT);
    if (project == null) {
      presentation.setEnabled(false);
      return;
    }
    FileEditor fileEditor = event.getData(PlatformDataKeys.FILE_EDITOR);
    Editor editor = event.getData(CommonDataKeys.EDITOR);
    presentation.setEnabled(fileEditor instanceof TextEditor
                            && !((TextEditor)fileEditor).getEditor().isOneLineMode()
                            && !SEARCH_DISABLED.get(editor, false));
  }
}
