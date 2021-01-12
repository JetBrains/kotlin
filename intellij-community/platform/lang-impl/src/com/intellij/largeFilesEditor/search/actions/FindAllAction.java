// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.largeFilesEditor.search.actions;

import com.intellij.icons.AllIcons;
import com.intellij.ide.IdeBundle;
import com.intellij.largeFilesEditor.search.LfeSearchManager;
import com.intellij.largeFilesEditor.search.searchTask.SearchTaskOptions;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.EditorBundle;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;

public class FindAllAction extends AnAction implements DumbAware {
  //private static final Logger logger = Logger.getInstance(FindAllAction.class);
  private final LfeSearchManager searchManager;

  public FindAllAction(LfeSearchManager searchManager) {
    this.searchManager = searchManager;

    getTemplatePresentation().setDescription(
      EditorBundle.message("large.file.editor.action.description.search.entire.file.and.show.toolwindow"));
    getTemplatePresentation().setText(IdeBundle.messagePointer("action.presentation.FindAllAction.text"));
    getTemplatePresentation().setIcon(AllIcons.Actions.FindEntireFile);
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    if (StringUtil.isEmpty(searchManager.getSearchReplaceComponent().getSearchTextComponent().getText())) {
      return;
    }

    searchManager.launchNewRangeSearch(SearchTaskOptions.NO_LIMIT, SearchTaskOptions.NO_LIMIT, true);
  }
}
