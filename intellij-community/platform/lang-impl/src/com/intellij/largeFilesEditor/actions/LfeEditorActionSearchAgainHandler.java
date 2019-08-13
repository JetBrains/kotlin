// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.largeFilesEditor.actions;

import com.intellij.largeFilesEditor.editor.EditorManager;
import com.intellij.largeFilesEditor.search.SearchManager;
import com.intellij.largeFilesEditor.search.searchTask.CloseSearchTask;
import com.intellij.largeFilesEditor.search.searchTask.SearchTaskBase;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LfeEditorActionSearchAgainHandler extends LfeBaseEditorActionHandler {

  private final boolean isForwardDirection = isForwardDirection();

  public LfeEditorActionSearchAgainHandler(EditorActionHandler originalHandler) {
    super(originalHandler);
  }

  @Override
  protected void doExecuteInLfe(@NotNull EditorManager editorManager,
                                @NotNull Editor editor,
                                @Nullable Caret caret,
                                DataContext dataContext) {
    SearchManager searchManager = editorManager.getSearchManager();
    searchManager.gotoNextOccurrence(isForwardDirection);
  }

  @Override
  protected boolean isEnabledInLfe(@NotNull EditorManager editorManager,
                                   @NotNull Editor editor,
                                   @NotNull Caret caret,
                                   DataContext dataContext) {
    SearchManager searchManager = editorManager.getSearchManager();
    SearchTaskBase task = searchManager.getLastExecutedSearchTask();
    return !(task instanceof CloseSearchTask && !task.isFinished());
  }

  protected boolean isForwardDirection() {
    return true;
  }
}
