// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.largeFilesEditor.editor;

import com.intellij.openapi.fileEditor.FileEditorState;
import com.intellij.openapi.fileEditor.FileEditorStateLevel;

class LargeFileEditorState implements FileEditorState {
  long caretPageNumber = 0;
  int caretSymbolOffsetInPage = 0;

  @Override
  public boolean canBeMergedWith(FileEditorState otherState, FileEditorStateLevel level) {
    if (otherState instanceof LargeFileEditorState) {
      LargeFileEditorState state = (LargeFileEditorState)otherState;
      return caretPageNumber == state.caretPageNumber
             && caretSymbolOffsetInPage == state.caretSymbolOffsetInPage;
    }
    return false;
  }

  @Override
  public String toString() {
    return "[p" + caretPageNumber + ",s" + caretSymbolOffsetInPage + "]";  // 'p' - Page, 's' - Symbol offset in Page
  }
}
