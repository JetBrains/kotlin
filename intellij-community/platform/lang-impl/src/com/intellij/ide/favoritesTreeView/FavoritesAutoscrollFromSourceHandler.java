// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.favoritesTreeView;

import com.intellij.ide.FileEditorSelectInContext;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.ui.AutoScrollFromSourceHandler;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * @author Konstantin Bulenkov
 */
public class FavoritesAutoscrollFromSourceHandler extends AutoScrollFromSourceHandler {
  private final FavoritesViewSelectInTarget mySelectInTarget = new FavoritesViewSelectInTarget(myProject);

  public FavoritesAutoscrollFromSourceHandler(@NotNull Project project, @NotNull FavoritesViewTreeBuilder builder) {
    super(project, Objects.requireNonNull(builder.getTree()), builder);
  }

  @Override
  protected boolean isAutoScrollEnabled() {
    return FavoritesManager.getInstance(myProject).getViewSettings().isAutoScrollFromSource();
  }

  @Override
  protected void setAutoScrollEnabled(boolean enabled) {
    FavoritesManager.getInstance(myProject).getViewSettings().setAutoScrollFromSource(enabled);
  }

  @Override
  protected void selectElementFromEditor(@NotNull FileEditor editor) {
    VirtualFile file = FileEditorManagerEx.getInstanceEx(myProject).getFile(editor);
    PsiFile psiFile = file == null ? null : PsiManager.getInstance(myProject).findFile(file);
    if (psiFile != null) new FileEditorSelectInContext(editor, psiFile).selectIn(mySelectInTarget, false);
  }
}