// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.largeFilesEditor.actions;

import com.intellij.largeFilesEditor.editor.LargeFileEditor;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class Utils {

  @Nullable
  static LargeFileEditor tryGetLargeFileEditorManager(AnActionEvent e) {
    FileEditor fileEditor = getFileEditor(e);
    if (fileEditor instanceof LargeFileEditor) {
      return (LargeFileEditor)fileEditor;
    }

    Editor editor = getEditor(e);
    return editor == null ? null : tryGetLargeFileEditorManagerFromEditor(editor);
  }

  @Nullable
  static LargeFileEditor tryGetLargeFileEditorManagerFromEditor(@NotNull Editor editor) {
    return editor.getUserData(LargeFileEditor.LARGE_FILE_EDITOR_KEY);
  }

  private static FileEditor getFileEditor(AnActionEvent e) {
    return e.getData(PlatformDataKeys.FILE_EDITOR);
  }

  private static Editor getEditor(AnActionEvent e) {
    return e.getData(CommonDataKeys.EDITOR);
  }
}
