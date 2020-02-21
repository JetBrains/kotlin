// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.largeFilesEditor.search;

import com.intellij.largeFilesEditor.editor.LargeFileEditor;
import com.intellij.largeFilesEditor.editor.LargeFileEditorProvider;
import com.intellij.largeFilesEditor.search.searchResultsPanel.RangeSearchCallback;
import com.intellij.largeFilesEditor.search.searchTask.FileDataProviderForSearch;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.EditorBundle;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;

public class RangeSearchCallbackImpl implements RangeSearchCallback {

  private static final Logger LOG = Logger.getInstance(RangeSearchCallbackImpl.class);

  @Override
  public FileDataProviderForSearch getFileDataProviderForSearch(boolean createIfNotExists, Project project, VirtualFile virtualFile) {
    LargeFileEditor largeFileEditor = getLargeFileEditor(createIfNotExists, project, virtualFile);
    return largeFileEditor == null ? null : largeFileEditor.getFileDataProviderForSearch();
  }

  @Override
  public void showResultInEditor(SearchResult searchResult, Project project, VirtualFile virtualFile) {
    LargeFileEditor largeFileEditor = getLargeFileEditor(true, project, virtualFile);
    if (largeFileEditor == null) {
      Messages.showWarningDialog(EditorBundle.message("large.file.editor.message.cant.show.file.in.the.editor"),
                                 EditorBundle.message("large.file.editor.title.show.match.problem"));
      LOG.info("[Large File Editor Subsystem] Can't get LargeFileEditor for showing search result. FilePath="
               + virtualFile.getPath());
      return;
    }
    largeFileEditor.showSearchResult(searchResult);

    // select necessary tab if any other is selected
    FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
    fileEditorManager.openFile(virtualFile, false, true);
    fileEditorManager.setSelectedEditor(virtualFile, LargeFileEditorProvider.PROVIDER_ID);
  }

  private static LargeFileEditor getLargeFileEditor(boolean createIfNotExists, Project project, VirtualFile virtualFile) {
    FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
    if (createIfNotExists) {
      FileEditor[] fileEditors = fileEditorManager.openFile(virtualFile, false, true);
      for (FileEditor fileEditor : fileEditors) {
        if (fileEditor instanceof LargeFileEditor) {
          return (LargeFileEditor)fileEditor;
        }
      }
    }
    else {
      FileEditor[] existedFileEditors = fileEditorManager.getEditors(virtualFile);
      for (FileEditor fileEditor : existedFileEditors) {
        if (fileEditor instanceof LargeFileEditor) {
          return (LargeFileEditor)fileEditor;
        }
      }
    }
    return null;
  }
}
