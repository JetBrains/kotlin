// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.generation.actions;

import com.intellij.codeInsight.CodeInsightActionHandler;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface PresentableCodeInsightActionHandler extends CodeInsightActionHandler {
  void update(@NotNull Editor editor, @NotNull PsiFile file, Presentation presentation);

  default void update(@NotNull Editor editor, @NotNull PsiFile file, Presentation presentation, @Nullable String actionPlace) {
    update(editor, file, presentation);
  }
}
