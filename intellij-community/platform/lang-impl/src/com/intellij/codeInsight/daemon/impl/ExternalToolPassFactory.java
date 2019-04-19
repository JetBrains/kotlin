// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.daemon.impl;

import com.intellij.codeHighlighting.*;
import com.intellij.lang.ExternalLanguageAnnotators;
import com.intellij.lang.Language;
import com.intellij.lang.annotation.ExternalAnnotator;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiFile;
import com.intellij.util.ui.update.MergingUpdateQueue;
import com.intellij.util.ui.update.Update;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ExternalToolPassFactory implements TextEditorHighlightingPassFactory, MainHighlightingPassFactory {
  private final MergingUpdateQueue myExternalActivitiesQueue;

  public ExternalToolPassFactory(Project project, TextEditorHighlightingPassRegistrar highlightingPassRegistrar) {
    // start after PostHighlightingPass completion since it could report errors that can prevent us to run
    highlightingPassRegistrar.registerTextEditorHighlightingPass(this, new int[]{Pass.UPDATE_ALL}, null, true, Pass.EXTERNAL_TOOLS);

    myExternalActivitiesQueue = new MergingUpdateQueue("ExternalActivitiesQueue", 300, true, MergingUpdateQueue.ANY_COMPONENT, project,
                                                       null, false);
    myExternalActivitiesQueue.setPassThrough(ApplicationManager.getApplication().isUnitTestMode());
  }

  @Override
  @Nullable
  public TextEditorHighlightingPass createHighlightingPass(@NotNull PsiFile file, @NotNull final Editor editor) {
    TextRange textRange = FileStatusMap.getDirtyTextRange(editor, Pass.EXTERNAL_TOOLS) == null ? null : file.getTextRange();
    if (textRange == null || !externalAnnotatorsDefined(file)) {
      return null;
    }
    return new ExternalToolPass(this, file, editor, textRange.getStartOffset(), textRange.getEndOffset());
  }

  private static boolean externalAnnotatorsDefined(@NotNull PsiFile file) {
    for (Language language : file.getViewProvider().getLanguages()) {
      final List<ExternalAnnotator> externalAnnotators = ExternalLanguageAnnotators.allForFile(language, file);
      if (!externalAnnotators.isEmpty()) {
        return true;
      }
    }
    return false;
  }

  void scheduleExternalActivity(@NotNull Update update) {
    myExternalActivitiesQueue.queue(update);
  }

  @Nullable
  @Override
  public TextEditorHighlightingPass createMainHighlightingPass(@NotNull PsiFile file,
                                                               @NotNull Document document,
                                                               @NotNull HighlightInfoProcessor highlightInfoProcessor) {
    TextRange range = file.getTextRange();
    if (range == null || !externalAnnotatorsDefined(file)) {
      return null;
    }
    return new ExternalToolPass(this, file, document, null, range.getStartOffset(), range.getEndOffset(), highlightInfoProcessor, true);
  }
}
