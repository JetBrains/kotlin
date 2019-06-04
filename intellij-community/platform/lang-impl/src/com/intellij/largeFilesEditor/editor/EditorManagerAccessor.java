// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.largeFilesEditor.editor;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

public interface EditorManagerAccessor {

  EditorManager getEditorManager(boolean createIfNotExists, Project project, VirtualFile virtualFile);

  void showEditorTab(@NotNull EditorManager editorManager);
}
