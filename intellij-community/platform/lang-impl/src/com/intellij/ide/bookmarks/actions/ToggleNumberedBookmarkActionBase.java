/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

/**
 * @author max
 */
public abstract class ToggleNumberedBookmarkActionBase extends AnAction implements DumbAware {
  private final int myNumber;

  public ToggleNumberedBookmarkActionBase(int n) {
    setEnabledInModalContext(true);
    myNumber = n;
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    e.getPresentation().setEnabled(e.getProject() != null);
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    if (project == null) return;

    BookmarksAction.BookmarkInContextInfo info = new BookmarksAction.BookmarkInContextInfo(e.getDataContext(), project).invoke();
    if (info.getFile() == null) return;

    Bookmark oldBookmark = info.getBookmarkAtPlace();
    BookmarkManager manager = BookmarkManager.getInstance(project);

    if (oldBookmark != null) {
      manager.removeBookmark(oldBookmark);
    }

    char mnemonic = (char)('0' + myNumber);
    if (oldBookmark == null || oldBookmark.getMnemonic() != mnemonic) {
      Bookmark bookmark = manager.addTextBookmark(info.getFile(), info.getLine(), "");
      manager.setMnemonic(bookmark, mnemonic);
    }
  }

  public static class ToggleBookmark0Action extends ToggleNumberedBookmarkActionBase {
    public ToggleBookmark0Action() {
      super(0);
    }
  }

  public static class ToggleBookmark1Action extends ToggleNumberedBookmarkActionBase {
    public ToggleBookmark1Action() {
      super(1);
    }
  }

  public static class ToggleBookmark2Action extends ToggleNumberedBookmarkActionBase {
    public ToggleBookmark2Action() {
      super(2);
    }
  }

  public static class ToggleBookmark3Action extends ToggleNumberedBookmarkActionBase {
    public ToggleBookmark3Action() {
      super(3);
    }
  }

  public static class ToggleBookmark4Action extends ToggleNumberedBookmarkActionBase {
    public ToggleBookmark4Action() {
      super(4);
    }
  }

  public static class ToggleBookmark5Action extends ToggleNumberedBookmarkActionBase {
    public ToggleBookmark5Action() {
      super(5);
    }
  }

  public static class ToggleBookmark6Action extends ToggleNumberedBookmarkActionBase {
    public ToggleBookmark6Action() {
      super(6);
    }
  }

  public static class ToggleBookmark7Action extends ToggleNumberedBookmarkActionBase {
    public ToggleBookmark7Action() {
      super(7);
    }
  }

  public static class ToggleBookmark8Action extends ToggleNumberedBookmarkActionBase {
    public ToggleBookmark8Action() {
      super(8);
    }
  }

  public static class ToggleBookmark9Action extends ToggleNumberedBookmarkActionBase {
    public ToggleBookmark9Action() {
      super(9);
    }
  }
}
