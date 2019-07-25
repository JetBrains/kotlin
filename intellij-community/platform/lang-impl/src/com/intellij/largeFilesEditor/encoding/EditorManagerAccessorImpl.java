// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.largeFilesEditor.encoding;

import com.intellij.largeFilesEditor.editor.EditorManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.impl.status.StatusBarUtil;
import org.jetbrains.annotations.Nullable;

public class EditorManagerAccessorImpl implements EditorManagerAccessor {
  @Nullable
  @Override
  public EditorManagerAccess getAccess(Project project, StatusBar statusBar) {
    if (project == null || project.isDisposed()) return null;

    FileEditor fileEditor = StatusBarUtil.getCurrentFileEditor(project, statusBar);
    if (fileEditor instanceof EditorManager) {
      return ((EditorManager)fileEditor).createAccessForEncodingWidget();
    }
    else {
      return null;
    }
  }
}
