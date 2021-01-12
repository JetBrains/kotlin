// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.largeFilesEditor.search.actions;

import com.intellij.icons.AllIcons;
import com.intellij.largeFilesEditor.search.searchResultsPanel.RangeSearch;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.EditorBundle;
import com.intellij.openapi.project.DumbAware;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class FindFurtherAction extends AnAction implements DumbAware {

  private static class Holder {
    private static final Icon ICON_FIND_ADD_NEXT = AllIcons.Actions.FindAndShowNextMatches;
    private static final Icon ICON_FIND_ADD_PREV = AllIcons.Actions.FindAndShowPrevMatches;
  }

  private final boolean directionForward;
  private final RangeSearch myRangeSearch;

  public FindFurtherAction(boolean directionForward, RangeSearch rangeSearch) {
    this.directionForward = directionForward;
    this.myRangeSearch = rangeSearch;

    String text;
    String description;
    Icon icon;

    if (directionForward) {
      text = EditorBundle.message("large.file.editor.find.further.forward.action.text");
      description = EditorBundle.message("large.file.editor.find.further.forward.action.description");
      icon = Holder.ICON_FIND_ADD_NEXT;
    }
    else {
      text = EditorBundle.message("large.file.editor.find.further.backward.action.text");
      description = EditorBundle.message("large.file.editor.find.further.backward.action.description");
      icon = Holder.ICON_FIND_ADD_PREV;
    }

    getTemplatePresentation().setText(text);
    getTemplatePresentation().setDescription(description);
    getTemplatePresentation().setIcon(icon);
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    boolean enabled = myRangeSearch.isButtonFindFurtherEnabled(directionForward);
    e.getPresentation().setEnabled(enabled);
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    myRangeSearch.onClickSearchFurther(directionForward);
  }
}
