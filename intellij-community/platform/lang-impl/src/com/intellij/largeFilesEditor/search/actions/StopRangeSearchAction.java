// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.largeFilesEditor.search.actions;

import com.intellij.icons.AllIcons;
import com.intellij.largeFilesEditor.editor.EditorManager;
import com.intellij.largeFilesEditor.editor.EditorManagerAccessor;
import com.intellij.largeFilesEditor.search.SearchManager;
import com.intellij.largeFilesEditor.search.searchResultsPanel.SearchResultsToolWindow;
import com.intellij.largeFilesEditor.search.searchTask.RangeSearchTask;
import com.intellij.largeFilesEditor.search.searchTask.SearchTaskBase;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class StopRangeSearchAction extends AnAction implements DumbAware {

  //private static final Logger logger = Logger.getInstance(FindFurtherAction.class);

  private static final String TEXT = "Stop Searching";
  private static final Icon ICON = AllIcons.Actions.Suspend;

  private final SearchResultsToolWindow searchResultsToolWindow;
  private final EditorManagerAccessor editorManagerAccessor;

  public StopRangeSearchAction(@NotNull SearchResultsToolWindow searchResultsToolWindow,
                               @NotNull EditorManagerAccessor editorManagerAccessor) {
    this.searchResultsToolWindow = searchResultsToolWindow;
    this.editorManagerAccessor = editorManagerAccessor;
    getTemplatePresentation().setText(TEXT);
    getTemplatePresentation().setIcon(ICON);
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    boolean enabled = false;
    Project project = searchResultsToolWindow.getProject();
    VirtualFile virtualFile = searchResultsToolWindow.getVirtualFile();
    EditorManager editorManager =
      editorManagerAccessor.getEditorManager(false, project, virtualFile);
    if (editorManager != null) {
      SearchManager searchManager = editorManager.getSearchManager();
      SearchTaskBase task = searchManager.getLastExecutedSearchTask();
      if (task instanceof RangeSearchTask
          && !task.isFinished()
          && !task.isShouldStop()) {
        enabled = true;
      }
    }
    e.getPresentation().setEnabled(enabled);
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Project project = searchResultsToolWindow.getProject();
    VirtualFile virtualFile = searchResultsToolWindow.getVirtualFile();
    EditorManager editorManager =
      editorManagerAccessor.getEditorManager(false, project, virtualFile);
    if (editorManager != null) {
      SearchManager searchManager = editorManager.getSearchManager();
      SearchTaskBase task = searchManager.getLastExecutedSearchTask();
      if (task instanceof RangeSearchTask) {
        searchManager.onEscapePressed();
      }
    }
  }
}