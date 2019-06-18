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
package com.intellij.ide.bookmarks;

import com.intellij.openapi.editor.markup.EffectType;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtilRt;
import com.intellij.openapi.vcs.FileStatus;
import com.intellij.openapi.vcs.FileStatusManager;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.ui.*;
import com.intellij.ui.popup.util.DetailView;
import com.intellij.ui.popup.util.ItemWrapper;

import javax.swing.*;
import java.awt.*;

/**
 * @author zajac
 */
public class BookmarkItem extends ItemWrapper implements Comparable<BookmarkItem>{
  private final Bookmark myBookmark;

  public BookmarkItem(Bookmark bookmark) {
    myBookmark = bookmark;
  }

  public Bookmark getBookmark() {
    return myBookmark;
  }

  @Override
  public void setupRenderer(ColoredListCellRenderer renderer, Project project, boolean selected) {
    setupRenderer(renderer, project, myBookmark, selected);
  }

  public static void setupRenderer(SimpleColoredComponent renderer, Project project, Bookmark bookmark, boolean selected) {
    VirtualFile file = bookmark.getFile();
    if (!file.isValid()) {
      return;
    }

    PsiElement fileOrDir = PsiUtilCore.findFileSystemItem(project, file);
    if (fileOrDir != null) {
      renderer.setIcon(fileOrDir.getIcon(0));
    }

    String description = bookmark.getDescription();
    if (!StringUtilRt.isEmptyOrSpaces(description)) {
      renderer.append(description + " ", SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES, true);
    }

    FileStatus fileStatus = FileStatusManager.getInstance(project).getStatus(file);
    TextAttributes attributes = new TextAttributes(fileStatus.getColor(), null, null, EffectType.LINE_UNDERSCORE, Font.PLAIN);
    renderer.append(file.getName(), SimpleTextAttributes.fromTextAttributes(attributes), true);
    if (bookmark.getLine() >= 0) {
      renderer.append(":", SimpleTextAttributes.GRAYED_ATTRIBUTES, true);
      renderer.append(String.valueOf(bookmark.getLine() + 1), SimpleTextAttributes.GRAYED_ATTRIBUTES, true);
    }
    renderer.append(" (" + VfsUtilCore.getRelativeLocation(file, project.getBaseDir()) + ")", SimpleTextAttributes.GRAY_ATTRIBUTES);

    if (!selected) {
      FileColorManager colorManager = FileColorManager.getInstance(project);
      if (fileOrDir instanceof PsiFile) {
        Color color = colorManager.getRendererBackground((PsiFile)fileOrDir);
        if (color != null) {
          renderer.setBackground(color);
        }
      }
    }
  }

  @Override
  public void setupRenderer(ColoredTreeCellRenderer renderer, Project project, boolean selected) {
    setupRenderer(renderer, project, myBookmark, selected);
  }

  @Override
  public void updateAccessoryView(JComponent component) {
    JLabel label = (JLabel)component;
    final char mnemonic = myBookmark.getMnemonic();
    if (mnemonic != 0) {
      label.setText(Character.toString(mnemonic) + '.');
    }
    else {
      label.setText("");
    }
  }

  @Override
  public String speedSearchText() {
    return myBookmark.getFile().getName() + " " + myBookmark.getDescription();
  }

  @Override
  public String footerText() {
    return myBookmark.getFile().getPresentableUrl();
  }

  @Override
  protected void doUpdateDetailView(DetailView panel, boolean editorOnly) {
    panel.navigateInPreviewEditor(DetailView.PreviewEditorState.create(myBookmark.getFile(), myBookmark.getLine()));
  }

  @Override
  public boolean allowedToRemove() {
    return true;
  }

  @Override
  public void removed(Project project) {
    BookmarkManager.getInstance(project).removeBookmark(getBookmark());
  }

  @Override
  public int compareTo(BookmarkItem o) {
    return myBookmark.compareTo(o.myBookmark);
  }
}
