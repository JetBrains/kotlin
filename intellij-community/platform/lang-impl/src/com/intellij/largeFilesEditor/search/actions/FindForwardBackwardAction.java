// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.largeFilesEditor.search.actions;

import com.intellij.icons.AllIcons;
import com.intellij.largeFilesEditor.search.SearchManager;
import com.intellij.largeFilesEditor.search.searchTask.SearchTaskOptions;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;

public class FindForwardBackwardAction extends AnAction implements DumbAware {

  private final SearchManager searchManager;
  private final boolean directionForward;

  public FindForwardBackwardAction(boolean directionForward, SearchManager searchManager) {
    this.directionForward = directionForward;
    this.searchManager = searchManager;

    getTemplatePresentation().setDescription(
      directionForward ?
      "Search from the current position to the end and show matching strings in the tool window" :
      "Search from the current position to the beginning and show matching strings in the tool window");
    getTemplatePresentation().setText(directionForward ? "Search Forward" : "Search Backward");
    getTemplatePresentation().setIcon(directionForward ? AllIcons.Actions.FindForward : AllIcons.Actions.FindBackward);
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    if (StringUtil.isEmpty(searchManager.getSearchManageGUI().getSearchTextComponent().getText())) {
      return;
    }
    searchManager.launchNewRangeSearch(
      directionForward ? searchManager.getEditorManager().getCaretPageNumber() : SearchTaskOptions.NO_LIMIT,
      directionForward ? SearchTaskOptions.NO_LIMIT : searchManager.getEditorManager().getCaretPageNumber(),
      directionForward);
  }
}
