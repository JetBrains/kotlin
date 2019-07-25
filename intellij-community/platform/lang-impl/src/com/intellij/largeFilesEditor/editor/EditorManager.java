// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.largeFilesEditor.editor;

import com.intellij.largeFilesEditor.encoding.EditorManagerAccess;
import com.intellij.largeFilesEditor.search.SearchManager;
import com.intellij.largeFilesEditor.search.SearchResult;
import com.intellij.largeFilesEditor.search.searchTask.FileDataProviderForSearch;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface EditorManager extends FileEditor {
  Key<Object> KEY_EDITOR_MARK = new Key<>("lfe.editorMark");
  Key<EditorManager> KEY_EDITOR_MANAGER = new Key<>("lfe.editorManager");

  SearchManager getSearchManager();

  void showSearchResult(SearchResult searchResult);

  Project getProject();

  long getCaretPageNumber();

  int getCaretPageOffset();

  Editor getEditor();

  @Override
  @Nullable
  VirtualFile getFile();

  @NotNull
  VirtualFile getVirtualFile();

  EditorManagerAccess createAccessForEncodingWidget();

  FileDataProviderForSearch getFileDataProviderForSearch();

  @NotNull
  EditorModel getEditorModel();
}
