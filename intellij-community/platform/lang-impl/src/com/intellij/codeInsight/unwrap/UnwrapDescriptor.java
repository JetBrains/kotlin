// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.unwrap;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Allows extending <em>Code | Unwrap/Remove</em>.
 *
 * @see UnwrapDescriptorBase
 */
public interface UnwrapDescriptor {

  @NotNull
  List<Pair<PsiElement, Unwrapper>> collectUnwrappers(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file);

  boolean showOptionsDialog();

  boolean shouldTryToRestoreCaretPosition();
}
