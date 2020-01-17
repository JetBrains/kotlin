// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.largeFilesEditor.search.searchResultsPanel;

import com.intellij.largeFilesEditor.search.SearchResult;
import com.intellij.largeFilesEditor.search.searchTask.FileDataProviderForSearch;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

public interface RangeSearchCallback {

  FileDataProviderForSearch getFileDataProviderForSearch(boolean createIfNotExists, Project project, VirtualFile virtualFile);

  void showResultInEditor(SearchResult searchResult, Project project, VirtualFile virtualFile);

}
