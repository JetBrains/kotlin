// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.largeFilesEditor.search.actions;

import com.intellij.icons.AllIcons;
import com.intellij.largeFilesEditor.search.searchResultsPanel.SearchResultsToolWindow;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAware;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class FindFurtherAction extends AnAction implements DumbAware {

  //private static final Logger logger = Logger.getInstance(FindFurtherAction.class);

  private static final String TEXT_FIND_NEXT = "Find Next Matches";
  private static final String TEXT_FIND_PREV = "Find Previous Matches";
  private static final String TEXT_FIND_ADD_NEXT = "Find and Add Next Matches";
  private static final String TEXT_FIND_ADD_PREV = "Find and Add Previous Matches";

  private static final String DESCRIPTION_FIND_NEXT =
    "Search for matches toward the end of the file and replace existing results";
  private static final String DESCRIPTION_FIND_PREV =
    "Search for matches toward the beginning of the file and replace existing results";
  private static final String DESCRIPTION_FIND_ADD_NEXT =
    "Search for matches toward the end of the file and add them to existing results";
  private static final String DESCRIPTION_FIND_ADD_PREV =
    "Search for matches toward the beginning of the file and add them to existing results";

  // TODO: 2019-05-17 remove 2 next positions if they won't be realy needed
  private static final Icon ICON_FIND_NEXT = AllIcons.Actions.Stub; // not used
  private static final Icon ICON_FIND_PREV = AllIcons.Actions.Stub; // not used
  private static final Icon ICON_FIND_ADD_NEXT = AllIcons.Actions.FindAndShowNextMatches;
  private static final Icon ICON_FIND_ADD_PREV = AllIcons.Actions.FindAndShowPrevMatches;

  private final boolean directionForward;
  private final boolean additionMode;
  private final SearchResultsToolWindow searchResultsToolWindow;

  public FindFurtherAction(boolean directionForward, boolean additionMode, SearchResultsToolWindow searchResultsToolWindow) {
    this.directionForward = directionForward;
    this.additionMode = additionMode;
    this.searchResultsToolWindow = searchResultsToolWindow;

    String text;
    String description;
    Icon icon;

    if (directionForward) {
      if (additionMode) {
        text = TEXT_FIND_ADD_NEXT;
        description = DESCRIPTION_FIND_ADD_NEXT;
        icon = ICON_FIND_ADD_NEXT;
      }
      else {
        text = TEXT_FIND_NEXT;
        description = DESCRIPTION_FIND_NEXT;
        icon = ICON_FIND_NEXT;
      }
    }
    else {
      if (additionMode) {
        text = TEXT_FIND_ADD_PREV;
        description = DESCRIPTION_FIND_ADD_PREV;
        icon = ICON_FIND_ADD_PREV;
      }
      else {
        text = TEXT_FIND_PREV;
        description = DESCRIPTION_FIND_PREV;
        icon = ICON_FIND_PREV;
      }
    }

    getTemplatePresentation().setText(text);
    getTemplatePresentation().setDescription(description);
    getTemplatePresentation().setIcon(icon);
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    boolean enabled = searchResultsToolWindow.isButtonFindFurtherEnabled(directionForward);
    e.getPresentation().setEnabled(enabled);
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    searchResultsToolWindow.onClickSearchFurther(directionForward, additionMode);
  }
}
