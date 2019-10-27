// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.actions;

import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ex.GlobalInspectionContextBase;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiFile;
import com.intellij.util.SmartList;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.concurrent.FutureTask;

public class CodeCleanupCodeProcessor extends AbstractLayoutCodeProcessor {
  public static final String COMMAND_NAME = "Cleanup code";
  public static final String PROGRESS_TEXT = CodeInsightBundle.message("process.cleanup.code");

  private SelectionModel mySelectionModel = null;

  public CodeCleanupCodeProcessor(@NotNull AbstractLayoutCodeProcessor previousProcessor) {
    super(previousProcessor, COMMAND_NAME, PROGRESS_TEXT);
  }

  public CodeCleanupCodeProcessor(@NotNull AbstractLayoutCodeProcessor previousProcessor, @NotNull SelectionModel selectionModel) {
    super(previousProcessor, COMMAND_NAME, PROGRESS_TEXT);
    mySelectionModel = selectionModel;
  }


  @NotNull
  @Override
  protected FutureTask<Boolean> prepareTask(@NotNull final PsiFile file, final boolean processChangedTextOnly) {
    return new FutureTask<>(() -> {
      Collection<TextRange> ranges = getRanges(file, processChangedTextOnly);
      GlobalInspectionContextBase.cleanupElements(myProject, null, descriptor -> isInRanges(ranges, descriptor), file);
      return true;
    });
  }

  private Collection<TextRange> getRanges(@NotNull PsiFile file, boolean processChangedTextOnly) {
    if (mySelectionModel != null) {
      return getSelectedRanges(mySelectionModel);
    }

    if (processChangedTextOnly) {
      return FormatChangedTextUtil.getInstance().getChangedTextRanges(myProject, file);
    }

    return new SmartList<>(file.getTextRange());
  }

  private static boolean isInRanges(Collection<? extends TextRange> ranges, @NotNull ProblemDescriptor descriptor) {
    for (TextRange range : ranges) {
      if (range.containsOffset(descriptor.getStartElement().getTextOffset())
          || range.containsOffset(descriptor.getEndElement().getTextOffset())) {
        return true;
      }
    }
    return false;
  }
}
