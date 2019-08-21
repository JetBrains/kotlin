// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.largeFilesEditor.search;

import com.intellij.largeFilesEditor.editor.EditorManager;
import com.intellij.largeFilesEditor.editor.LargeFileEditorProvider;
import com.intellij.largeFilesEditor.search.searchResultsPanel.RangeSearchCallback;
import com.intellij.largeFilesEditor.search.searchTask.FileDataProviderForSearch;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;

public class RangeSearchCallbackImpl implements RangeSearchCallback {

  private static final Logger LOG = Logger.getInstance(RangeSearchCallbackImpl.class);

  @Override
  public FileDataProviderForSearch getFileDataProviderForSearch(boolean createIfNotExists, Project project, VirtualFile virtualFile) {
    EditorManager editorManager = getEditorManager(createIfNotExists, project, virtualFile);
    return editorManager == null ? null : editorManager.getFileDataProviderForSearch();
  }

  @Override
  public void showResultInEditor(SearchResult searchResult, Project project, VirtualFile virtualFile) {
    EditorManager editorManager = getEditorManager(true, project, virtualFile);
    if (editorManager == null) {
      Messages.showWarningDialog("Can't show file in the editor", "Show Match Problem");
      LOG.info("[Large File Editor Subsystem] Can't get EditorManager for showing search result. FilePath="
               + virtualFile.getPath());
      return;
    }
    editorManager.showSearchResult(searchResult);

    // select necessary tab if any other is selected
    FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
    fileEditorManager.openFile(virtualFile, false, true);
    fileEditorManager.setSelectedEditor(virtualFile, LargeFileEditorProvider.PROVIDER_ID);
  }

  private static EditorManager getEditorManager(boolean createIfNotExists, Project project, VirtualFile virtualFile) {
    FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
    if (createIfNotExists) {
      FileEditor[] fileEditors = fileEditorManager.openFile(virtualFile, false, true);
      for (FileEditor fileEditor : fileEditors) {
        if (fileEditor instanceof EditorManager) {
          return (EditorManager)fileEditor;
        }
      }
    }
    else {
      FileEditor[] existedFileEditors = fileEditorManager.getEditors(virtualFile);
      for (FileEditor fileEditor : existedFileEditors) {
        if (fileEditor instanceof EditorManager) {
          return (EditorManager)fileEditor;
        }
      }
    }
    return null;
  }
}
