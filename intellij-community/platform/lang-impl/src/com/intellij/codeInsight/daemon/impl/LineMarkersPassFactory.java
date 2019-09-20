// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.daemon.impl;

import com.intellij.codeHighlighting.*;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.impl.MarkupModelImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.ProperTextRange;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class LineMarkersPassFactory implements TextEditorHighlightingPassFactory, TextEditorHighlightingPassFactoryRegistrar {
  @Override
  public void registerHighlightingPassFactory(@NotNull TextEditorHighlightingPassRegistrar registrar, @NotNull Project project) {
    registrar.registerTextEditorHighlightingPass(this, null, new int[]{Pass.UPDATE_ALL}, false, Pass.LINE_MARKERS);
  }

  @NotNull
  @Override
  public TextEditorHighlightingPass createHighlightingPass(@NotNull PsiFile file, @NotNull final Editor editor) {
    TextRange restrictRange = FileStatusMap.getDirtyTextRange(editor, Pass.LINE_MARKERS);
    Document document = editor.getDocument();
    Project project = file.getProject();
    if (restrictRange == null) return new ProgressableTextEditorHighlightingPass.EmptyPass(project, document);
    ProperTextRange visibleRange = VisibleHighlightingPassFactory.calculateVisibleRange(editor);
    return new LineMarkersPass(project, file, document, expandRangeToCoverWholeLines(document, visibleRange), expandRangeToCoverWholeLines(document, restrictRange));
  }

  @Nullable
  private static TextRange expandRangeToCoverWholeLines(@NotNull Document document, TextRange textRange) {
    if (textRange == null) {
      return null;
    }
    return MarkupModelImpl.roundToLineBoundaries(document, textRange.getStartOffset(), textRange.getEndOffset());
  }
}