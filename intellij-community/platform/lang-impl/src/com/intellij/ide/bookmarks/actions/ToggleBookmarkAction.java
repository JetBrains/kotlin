// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.bookmarks.actions;

import com.intellij.icons.AllIcons;
import com.intellij.ide.IdeBundle;
import com.intellij.ide.bookmarks.BookmarkManager;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class ToggleBookmarkAction extends BookmarksAction implements DumbAware, Toggleable {
  public ToggleBookmarkAction() {
    getTemplatePresentation().setText(IdeBundle.message("action.bookmark.toggle"));
  }

  @Override
  public void update(@NotNull AnActionEvent event) {
    Project project = event.getProject();
    DataContext dataContext = event.getDataContext();
    event.getPresentation().setEnabled(project != null &&
                                       (CommonDataKeys.EDITOR.getData(dataContext) != null ||
                                        CommonDataKeys.VIRTUAL_FILE.getData(dataContext) != null));

    if (ActionPlaces.TOUCHBAR_GENERAL.equals(event.getPlace())) {
      event.getPresentation().setIcon(AllIcons.Actions.Checked);
    }

    final BookmarkInContextInfo info = getBookmarkInfo(event);
    final boolean selected = info != null && info.getBookmarkAtPlace() != null;
    if (ActionPlaces.isPopupPlace(event.getPlace())) {
      event.getPresentation().setText(selected ? "Clear Bookmark" : "Set Bookmark");
    }
    else {
      event.getPresentation().setText(IdeBundle.message("action.bookmark.toggle"));
      Toggleable.setSelected(event.getPresentation(), selected);
    }
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    if (project == null) return;

    final BookmarkInContextInfo info = getBookmarkInfo(e);
    if (info == null) return;

    final boolean selected = info.getBookmarkAtPlace() != null;
    Toggleable.setSelected(e.getPresentation(), selected);

    if (selected) {
      BookmarkManager.getInstance(project).removeBookmark(info.getBookmarkAtPlace());
    }
    else {
      BookmarkManager.getInstance(project).addTextBookmark(info.getFile(), info.getLine(), "");
    }
  }

  public static BookmarkInContextInfo getBookmarkInfo(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    if (project == null) return null;

    final BookmarkInContextInfo info = new BookmarkInContextInfo(e.getDataContext(), project).invoke();
    return info.getFile() == null ? null : info;
  }
}
