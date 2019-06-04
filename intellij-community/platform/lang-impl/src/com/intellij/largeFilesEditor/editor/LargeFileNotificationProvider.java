// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.largeFilesEditor.editor;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.EditorNotificationPanel;
import com.intellij.ui.EditorNotifications;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class LargeFileNotificationProvider extends EditorNotifications.Provider {
  private static final Key<EditorNotificationPanel> KEY = Key.create("large.file.editor.notification");
  private static final Key<String> HIDDEN_KEY = Key.create("large.file.editor.notification.hidden");
  private static final String DISABLE_KEY = "large.file.editor.notification.disabled";

  @NotNull
  @Override
  public Key<EditorNotificationPanel> getKey() {
    return KEY;
  }

  @Nullable
  @Override
  public EditorNotificationPanel createNotificationPanel(@NotNull VirtualFile file,
                                                         @NotNull FileEditor fileEditor,
                                                         @NotNull Project project) {
    if (!(fileEditor instanceof EditorManager)) return null;
    Editor editor = ((EditorManager)fileEditor).getEditor();
    if (editor.getUserData(HIDDEN_KEY) != null || PropertiesComponent.getInstance().isTrueValue(DISABLE_KEY)) {
      return null;
    }

    EditorNotificationPanel panel = new EditorNotificationPanel();
    panel.createActionLabel("Hide notification", () -> {
      editor.putUserData(HIDDEN_KEY, "true");
      update(file, project);
    });
    panel.createActionLabel("Don't show again", () -> {
      PropertiesComponent.getInstance().setValue(DISABLE_KEY, "true");
      update(file, project);
    });
    return panel.text(String.format(
      "The file is too large: %s. It is showing by a special read-only viewer for large files.",
      StringUtil.formatFileSize(file.getLength())));
  }

  private static void update(@NotNull VirtualFile file, @NotNull Project project) {
    EditorNotifications.getInstance(project).updateNotifications(file);
  }
}
