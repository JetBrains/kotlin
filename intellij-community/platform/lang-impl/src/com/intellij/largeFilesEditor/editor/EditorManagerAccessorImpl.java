// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.largeFilesEditor.editor;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

public class EditorManagerAccessorImpl implements EditorManagerAccessor {
  @Override
  public EditorManager getEditorManager(boolean createIfNotExists, Project project, VirtualFile virtualFile) {
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

  @Override
  public void showEditorTab(@NotNull EditorManager editorManager) {
    FileEditorManager fileEditorManager = FileEditorManager.getInstance(editorManager.getProject());
    VirtualFile virtualFile = editorManager.getVirtualFile();
    fileEditorManager.openFile(virtualFile, false, true);
    fileEditorManager.setSelectedEditor(virtualFile, LargeFileEditorProvider.PROVIDER_ID);
  }
}
