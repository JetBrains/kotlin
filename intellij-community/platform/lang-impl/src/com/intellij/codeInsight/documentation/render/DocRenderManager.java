// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.documentation.render;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.ex.EditorSettingsExternalizable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.util.Key;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class DocRenderManager {
  private static final Key<Boolean> DOC_RENDER_ENABLED = Key.create("doc.render.enabled");

  /**
   * Allows to override global doc comments rendering setting for a specific editor. Passing {@code null} as {@code value} makes editor use
   * the global setting again.
   */
  public static void setDocRenderingEnabled(@NotNull Editor editor, @Nullable Boolean value) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    boolean enabledBefore = isDocRenderingEnabled(editor);
    editor.putUserData(DOC_RENDER_ENABLED, value);
    boolean enabledAfter = isDocRenderingEnabled(editor);
    if (enabledAfter != enabledBefore) {
      resetEditorToDefaultState(editor);
    }
  }

  /**
   * Tells whether doc comment rendering is enabled for a specific editor.
   *
   * @see #setDocRenderingEnabled(Editor, Boolean)
   */
  public static boolean isDocRenderingEnabled(@NotNull Editor editor) {
    Boolean value = editor.getUserData(DOC_RENDER_ENABLED);
    return value == null ? EditorSettingsExternalizable.getInstance().isDocCommentRenderingEnabled() : value;
  }

  /**
   * Sets all doc comments to their default state (rendered or not rendered) for all opened editors.
   *
   * @see #isDocRenderingEnabled(Editor)
   */
  public static void resetAllEditorsToDefaultState() {
    ApplicationManager.getApplication().assertIsDispatchThread();
    for (Editor editor : EditorFactory.getInstance().getAllEditors()) {
      DocRenderItem.resetToDefaultState(editor);
      DocRenderPassFactory.forceRefreshOnNextPass(editor);
    }
    for (Project project : ProjectManager.getInstance().getOpenProjects()) {
      DaemonCodeAnalyzer.getInstance(project).restart();
    }
  }

  /**
   * Sets all doc comments to their default state (rendered or not rendered) in the specified editor.
   *
   * @see #isDocRenderingEnabled(Editor)
   */
  public static void resetEditorToDefaultState(@NotNull Editor editor) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    DocRenderItem.resetToDefaultState(editor);
    DocRenderPassFactory.forceRefreshOnNextPass(editor);
    Project project = editor.getProject();
    if (project != null) {
      DaemonCodeAnalyzer.getInstance(project).restart();
    }
  }
}
