// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.largeFilesEditor.search;

import com.intellij.largeFilesEditor.search.searchResultsPanel.SearchResultsToolWindow;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

public interface SearchResultsPanelManagerAccessor {

  SearchResultsToolWindow getSearchResultsToolWindow(boolean createIfNotExists,
                                                     Project project, VirtualFile virtualFile);

  void showSearchResultsToolWindow(@NotNull SearchResultsToolWindow searchResultsToolWindow);

  void closeToolWindowTab(VirtualFile virtualFile, Project project);
}
