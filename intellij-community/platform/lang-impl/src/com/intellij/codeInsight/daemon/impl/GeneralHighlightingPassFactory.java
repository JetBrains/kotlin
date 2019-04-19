// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.daemon.impl;

import com.intellij.codeHighlighting.MainHighlightingPassFactory;
import com.intellij.codeHighlighting.Pass;
import com.intellij.codeHighlighting.TextEditorHighlightingPass;
import com.intellij.codeHighlighting.TextEditorHighlightingPassRegistrar;
import com.intellij.codeInsight.daemon.impl.ProgressableTextEditorHighlightingPass.EmptyPass;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.ProperTextRange;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

final class GeneralHighlightingPassFactory implements MainHighlightingPassFactory {
  private final Project myProject;

  GeneralHighlightingPassFactory(@NotNull Project project, @NotNull TextEditorHighlightingPassRegistrar highlightingPassRegistrar) {
    myProject = project;
    highlightingPassRegistrar.registerTextEditorHighlightingPass(this,
                                                                 null,
                                                                 new int[]{Pass.UPDATE_FOLDING}, false, Pass.UPDATE_ALL);
  }

  @NotNull
  @Override
  public TextEditorHighlightingPass createHighlightingPass(@NotNull PsiFile file, @NotNull final Editor editor) {
    TextRange textRange = FileStatusMap.getDirtyTextRange(editor, Pass.UPDATE_ALL);
    if (textRange == null) return new EmptyPass(myProject, editor.getDocument());
    ProperTextRange visibleRange = VisibleHighlightingPassFactory.calculateVisibleRange(editor);
    return new GeneralHighlightingPass(myProject, file, editor.getDocument(), textRange.getStartOffset(), textRange.getEndOffset(), true, visibleRange, editor, new DefaultHighlightInfoProcessor());
  }

  @Override
  public TextEditorHighlightingPass createMainHighlightingPass(@NotNull PsiFile file,
                                                               @NotNull Document document,
                                                               @NotNull HighlightInfoProcessor highlightInfoProcessor) {
    // no applying to the editor - for read-only analysis only
    return new GeneralHighlightingPass(myProject, file, document, 0, file.getTextLength(),
                                       true, new ProperTextRange(0, document.getTextLength()), null, highlightInfoProcessor);
  }
}
