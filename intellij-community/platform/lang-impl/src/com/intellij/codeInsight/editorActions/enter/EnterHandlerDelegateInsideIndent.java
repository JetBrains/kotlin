// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.editorActions.enter;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import org.jetbrains.annotations.NotNull;

public interface EnterHandlerDelegateInsideIndent extends EnterHandlerDelegate {
  boolean needCustomPreprocessingInsideIndent(int newLineCharOffset,
                                              @NotNull Editor editor,
                                              @NotNull final DataContext dataContext);
}
