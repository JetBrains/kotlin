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

public class FindAllAction extends AnAction implements DumbAware {
  //private static final Logger logger = Logger.getInstance(FindAllAction.class);
  private final SearchManager searchManager;

  public FindAllAction(SearchManager searchManager) {
    this.searchManager = searchManager;

    getTemplatePresentation().setDescription("Search the whole file from the beginning " +
                                             "and show matching strings in the tool window");
    getTemplatePresentation().setText("Search All");
    getTemplatePresentation().setIcon(AllIcons.Actions.FindEntireFile);
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    if (StringUtil.isEmpty(searchManager.getSearchManageGUI().getSearchTextComponent().getText())) {
      return;
    }

    searchManager.launchNewRangeSearch(SearchTaskOptions.NO_LIMIT, SearchTaskOptions.NO_LIMIT, true);
  }
}
