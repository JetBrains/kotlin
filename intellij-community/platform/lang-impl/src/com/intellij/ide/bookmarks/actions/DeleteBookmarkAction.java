/*
 * Copyright 2000-2017 JetBrains s.r.o.
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

import com.intellij.icons.AllIcons;
import com.intellij.ide.bookmarks.Bookmark;
import com.intellij.ide.bookmarks.BookmarkItem;
import com.intellij.ide.bookmarks.BookmarkManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CustomShortcutSet;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.ui.ListUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.List;

class DeleteBookmarkAction extends DumbAwareAction {
  private final Project myProject;
  private final JList<? extends BookmarkItem> myList;

  DeleteBookmarkAction(Project project, JList<? extends BookmarkItem> list) {
    super("Delete", "Delete current bookmark", AllIcons.General.Remove);
    setEnabledInModalContext(true);
    myProject = project;
    myList = list;
    registerCustomShortcutSet(CustomShortcutSet.fromString("DELETE", "BACK_SPACE"), list);
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    e.getPresentation().setEnabled(BookmarksAction.getSelectedBookmarks(myList).size() > 0);
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    List<Bookmark> bookmarks = BookmarksAction.getSelectedBookmarks(myList);
    ListUtil.removeSelectedItems(myList);

    for (Bookmark bookmark : bookmarks) {
      BookmarkManager.getInstance(myProject).removeBookmark(bookmark);
    }
  }
}