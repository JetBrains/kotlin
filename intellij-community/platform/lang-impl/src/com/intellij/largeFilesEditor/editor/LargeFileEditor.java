// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.largeFilesEditor.editor;

import com.intellij.largeFilesEditor.encoding.LargeFileEditorAccess;
import com.intellij.largeFilesEditor.search.LfeSearchManager;
import com.intellij.largeFilesEditor.search.SearchResult;
import com.intellij.largeFilesEditor.search.searchTask.FileDataProviderForSearch;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

public interface LargeFileEditor extends FileEditor {
  Key<Object> LARGE_FILE_EDITOR_MARK_KEY = new Key<>("lfe.editorMark");
  Key<LargeFileEditor> LARGE_FILE_EDITOR_KEY = new Key<>("lfe.editor");

  LfeSearchManager getSearchManager();

  void showSearchResult(SearchResult searchResult);

  Project getProject();

  long getCaretPageNumber();

  int getCaretPageOffset();

  Editor getEditor();

  @Override
  @NotNull
  VirtualFile getFile();

  LargeFileEditorAccess createAccessForEncodingWidget();

  FileDataProviderForSearch getFileDataProviderForSearch();

  @NotNull
  EditorModel getEditorModel();

  int getPageSize();
}
