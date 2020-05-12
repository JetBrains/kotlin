// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.analysis.problemsView.toolWindow;

import com.intellij.openapi.editor.*;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class ProblemsViewPreview {
  private final ProblemsViewPanel panel;
  private Editor editor;

  ProblemsViewPreview(@NotNull ProblemsViewPanel panel) {
    this.panel = panel;
  }

  void preview(@Nullable OpenFileDescriptor descriptor) {
    Document document = descriptor == null ? null : ProblemsView.getDocument(panel.getProject(), descriptor.getFile());
    if (editor != null && document != editor.getDocument()) {
      panel.setSecondComponent(null);
      EditorFactory.getInstance().releaseEditor(editor);
      this.editor = null;
    }
    if (editor == null && document != null) {
      editor = EditorFactory.getInstance().createViewer(document, panel.getProject(), EditorKind.PREVIEW);

      EditorSettings settings = editor.getSettings();
      settings.setAnimatedScrolling(false);
      settings.setRefrainFromScrolling(false);
      settings.setLineNumbersShown(true);
      settings.setFoldingOutlineShown(false);

      editor.setBorder(null);
      panel.setSecondComponent(editor.getComponent());
    }
    if (editor != null) {
      descriptor.navigateIn(editor);
    }
  }
}
