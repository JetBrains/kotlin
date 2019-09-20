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

package com.intellij.ide.bookmarks.actions;

import com.intellij.ide.bookmarks.Bookmark;
import com.intellij.ide.bookmarks.BookmarkManager;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.editor.actionSystem.EditorAction;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

abstract class GotoBookmarkActionBase extends EditorAction {
  protected GotoBookmarkActionBase(final boolean next) {
    super(new EditorActionHandler() {
      @Override
      public void execute(@NotNull Editor editor, DataContext dataContext) {
        navigateToBookmark(dataContext, editor);
      }

      @Override
      public boolean isEnabled(Editor editor, DataContext dataContext) {
        return getBookmarkToGo(dataContext, editor) != null;
      }

      private void navigateToBookmark(DataContext dataContext, @NotNull final Editor editor) {
        final Bookmark bookmark = getBookmarkToGo(dataContext, editor);
        if (bookmark == null) return;

        int line = bookmark.getLine();
        if (line >= editor.getDocument().getLineCount()) return;
        if (line < 0) line = 0;

        LogicalPosition pos = new LogicalPosition(line, 0);
        editor.getSelectionModel().removeSelection();
        editor.getCaretModel().removeSecondaryCarets();
        editor.getCaretModel().moveToLogicalPosition(pos);
        editor.getScrollingModel().scrollTo(new LogicalPosition(line, 0), ScrollType.CENTER);
      }

      @Nullable
      private Bookmark getBookmarkToGo(DataContext dataContext, @NotNull Editor editor) {
        Project project = CommonDataKeys.PROJECT.getData(dataContext);
        if (project == null) return null;
        return BookmarkManager.getInstance(project).findLineBookmark(editor, true, next);
      }
    });
  }
}
