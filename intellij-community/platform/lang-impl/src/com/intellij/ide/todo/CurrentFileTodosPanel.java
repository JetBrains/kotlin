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
package com.intellij.ide.todo;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.ui.content.Content;
import org.jetbrains.annotations.NotNull;

abstract class CurrentFileTodosPanel extends TodoPanel {
  CurrentFileTodosPanel(Project project, TodoPanelSettings settings, Content content) {
    super(project, settings, true, content);

    VirtualFile[] files = FileEditorManager.getInstance(project).getSelectedFiles();
    setFile(files.length == 0 ? null : PsiManager.getInstance(myProject).findFile(files[0]), true);
    // It's important to remove this listener. It prevents invocation of setFile method after the tree builder is disposed
    project.getMessageBus().connect(this).subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, new FileEditorManagerListener() {
      @Override
      public void selectionChanged(@NotNull FileEditorManagerEvent e) {
        VirtualFile file = e.getNewFile();
        final PsiFile psiFile = file != null && file.isValid() ? PsiManager.getInstance(myProject).findFile(file) : null;
        // This invokeLater is required. The problem is setFile does a commit to PSI, but setFile is
        // invoked inside PSI change event. It causes an Exception like "Changes to PSI are not allowed inside event processing"
        ApplicationManager.getApplication().invokeLater(() -> setFile(psiFile, false));
      }
    });
  }

  private void setFile(PsiFile file, boolean initialUpdate) {
    // setFile method is invoked in LaterInvocator so PsiManager
    // can be already disposed, so we need to check this before using it.
    if (myProject == null || PsiManager.getInstance(myProject).isDisposed()) {
      return;
    }

    if (file != null && getSelectedFile() == file) return;

    CurrentFileTodosTreeBuilder builder = (CurrentFileTodosTreeBuilder)myTodoTreeBuilder;
    builder.setFile(file);
    if (myTodoTreeBuilder.isUpdatable() || initialUpdate) {
      Object selectableElement = builder.getTodoTreeStructure().getFirstSelectableElement();
      if (selectableElement != null) {
        builder.select(selectableElement);
      }
    }
  }
}
