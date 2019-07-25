// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.ide.util;

import com.intellij.ide.ui.UISettings;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EditorHelper {
  public static <T extends PsiElement> void openFilesInEditor(@NotNull T[] elements) {
    final int limit = UISettings.getInstance().getEditorTabLimit();
    final int max = Math.min(limit, elements.length);
    for (int i = 0; i < max; i++) {
      openInEditor(elements[i], true);
    }
  }

  public static Editor openInEditor(@NotNull PsiElement element) {
    FileEditor editor = openInEditor(element, true);
    return editor instanceof TextEditor ? ((TextEditor)editor).getEditor() : null;
  }

  @Nullable
  public static FileEditor openInEditor(@NotNull PsiElement element, boolean switchToText) {
    return openInEditor(element, switchToText, false);
  }

  @Nullable
  public static FileEditor openInEditor(@NotNull PsiElement element, boolean switchToText, boolean focusEditor) {
    PsiFile file;
    int offset;
    if (element instanceof PsiFile){
      file = (PsiFile)element;
      offset = -1;
    }
    else{
      file = element.getContainingFile();
      offset = element.getTextOffset();
    }
    if (file == null) return null;//SCR44414
    VirtualFile virtualFile = file.getVirtualFile();
    if (virtualFile == null) return null;
    OpenFileDescriptor descriptor = new OpenFileDescriptor(element.getProject(), virtualFile, offset);
    Project project = element.getProject();
    if (offset == -1 && !switchToText) {
      FileEditorManager.getInstance(project).openEditor(descriptor, focusEditor);
    }
    else {
      FileEditorManager.getInstance(project).openTextEditor(descriptor, focusEditor);
    }
    return FileEditorManager.getInstance(project).getSelectedEditor(virtualFile);
  }
}