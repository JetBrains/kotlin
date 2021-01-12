// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.bookmarks.actions;

import com.intellij.ide.IdeBundle;
import com.intellij.ide.bookmarks.Bookmark;
import com.intellij.ide.bookmarks.BookmarkManager;
import com.intellij.lang.LangBundle;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.editor.VisualPosition;
import com.intellij.openapi.editor.ex.EditorGutterComponentEx;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.JBPopupListener;
import com.intellij.openapi.ui.popup.LightweightWindowEvent;
import com.intellij.ui.popup.PopupFactoryImpl;
import com.intellij.util.ui.StartupUiUtil;
import org.jetbrains.annotations.NotNull;

public class ToggleBookmarkWithMnemonicAction extends ToggleBookmarkAction {
  private boolean myPopupShown;

  public ToggleBookmarkWithMnemonicAction() {
    getTemplatePresentation().setText(IdeBundle.messagePointer("action.bookmark.toggle.mnemonic"));
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    super.update(e);
    e.getPresentation().setEnabled(!myPopupShown);

    final BookmarkInContextInfo info = getBookmarkInfo(e);
    if (ActionPlaces.isPopupPlace(e.getPlace())) {
      if (info != null && info.getBookmarkAtPlace() != null) {
        e.getPresentation().setVisible(false);
      }
      else {
        e.getPresentation().setText(IdeBundle.messagePointer("action.presentation.ToggleBookmarkWithMnemonicAction.text"));
      }
    }
    else {
      e.getPresentation().setText(IdeBundle.messagePointer("action.bookmark.toggle.mnemonic"));
    }
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    super.actionPerformed(e);

    final Project project = e.getProject();
    if (project == null) return;

    final BookmarkInContextInfo info = new BookmarkInContextInfo(e.getDataContext(), project).invoke();
    final Bookmark bookmark = info.getBookmarkAtPlace();
    final BookmarkManager bookmarks = BookmarkManager.getInstance(project);
    if (bookmark != null) {
      final Editor editor = e.getData(CommonDataKeys.EDITOR);
      if (editor != null) {
        Integer gutterLineAtCursor = e.getData(EditorGutterComponentEx.LOGICAL_LINE_AT_CURSOR);
        if (gutterLineAtCursor != null) {
          VisualPosition position = editor.logicalToVisualPosition(new LogicalPosition(gutterLineAtCursor, 0));
          editor.putUserData(PopupFactoryImpl.ANCHOR_POPUP_POSITION, position);
        }
      }

      final JBPopup[] popup = new JBPopup[1];

      MnemonicChooser mc = new MnemonicChooser() {
        @Override
        protected void onMnemonicChosen(char c) {
          popup[0].cancel();
          bookmarks.setMnemonic(bookmark, c);
        }

        @Override
        protected void onCancelled() {
          popup[0].cancel();
          bookmarks.removeBookmark(bookmark);
        }

        @Override
        protected boolean isOccupied(char c) {
          return bookmarks.findBookmarkForMnemonic(c) != null;
        }
      };

      popup[0] = JBPopupFactory.getInstance().createComponentPopupBuilder(mc, mc).
        setTitle(LangBundle.message("popup.title.bookmark.mnemonic")).
        setFocusable(true).
        setRequestFocus(true).
        setMovable(false).
        setCancelKeyEnabled(false).
        setAdText(bookmarks.hasBookmarksWithMnemonics()
                  ? LangBundle.message("popup.advertisement.cells.are.in.use",
                                       StartupUiUtil.isUnderDarcula() ?
                                       LangBundle.message("popup.advertisement.cells.are.in.use.brown") :
                                       LangBundle.message("popup.advertisement.cells.are.in.use.yellow")) : null).
        setResizable(false).
        createPopup();
      popup[0].addListener(new JBPopupListener() {
        @Override
        public void onClosed(@NotNull LightweightWindowEvent event) {
          myPopupShown = false;
          if (editor != null) {
            editor.putUserData(PopupFactoryImpl.ANCHOR_POPUP_POSITION, null);
          }
        }
      });

      popup[0].showInBestPositionFor(e.getDataContext());
      myPopupShown = true;
    }
  }
}
