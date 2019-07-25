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
import com.intellij.ide.bookmarks.BookmarkItem;
import com.intellij.ide.bookmarks.BookmarkManager;
import com.intellij.ide.ui.UISettings;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonShortcuts;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.ui.ListUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

class MoveBookmarkUpAction extends DumbAwareAction {
  private final Project myProject;
  private final JList<BookmarkItem> myList;

  MoveBookmarkUpAction(Project project, JList<BookmarkItem> list) {
    super("Up", "Move current bookmark up", AllIcons.Actions.PreviousOccurence);
    setEnabledInModalContext(true);
    myProject = project;
    myList = list;
    registerCustomShortcutSet(CommonShortcuts.MOVE_UP, list);
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    e.getPresentation().setEnabled(
      !UISettings.getInstance().getSortBookmarks() &&
      BookmarksAction.notFiltered(myList)
      && BookmarksAction.getSelectedBookmarks(myList).size() == 1
      && myList.getSelectedIndex() > 0);
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    ListUtil.moveSelectedItemsUp(myList);
    BookmarkManager.getInstance(myProject).moveBookmarkUp(BookmarksAction.getSelectedBookmarks(myList).get(0));
  }
}