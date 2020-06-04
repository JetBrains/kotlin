/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.intellij.slicer;

import com.intellij.codeInsight.CodeInsightActionHandler;
import com.intellij.codeInsight.TargetElementUtil;
import com.intellij.codeInsight.hint.HintManager;
import com.intellij.lang.LangBundle;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class SliceHandler implements CodeInsightActionHandler {
  private static final Logger LOG = Logger.getInstance(SliceHandler.class);
  final boolean myDataFlowToThis;

  SliceHandler(boolean dataFlowToThis) {
    myDataFlowToThis = dataFlowToThis;
  }

  @Override
  public void invoke(@NotNull final Project project, @NotNull final Editor editor, @NotNull final PsiFile file) {
    PsiElement expression = getExpressionAtCaret(editor, file);
    if (expression == null) {
      HintManager.getInstance().showErrorHint(editor, LangBundle.message("hint.text.cannot.find.what.to.analyze"));
      return;
    }

    if (!expression.isPhysical()) {
      PsiFile expressionFile = expression.getContainingFile();
      LOG.error("Analyzed entity should be physical. " +
                "Analyzed element: " + expression.getText() + " (class = " + expression.getClass() + "), file = " + file +
                " expression file = " + expressionFile + " (class = " + expressionFile.getClass() + ")");
    }
    SliceManager sliceManager = SliceManager.getInstance(project);
    sliceManager.slice(expression,myDataFlowToThis, this);
  }

  @Override
  public boolean startInWriteAction() {
    return false;
  }

  @Nullable
  public PsiElement getExpressionAtCaret(final Editor editor, final PsiFile file) {
    int offset = TargetElementUtil.adjustOffset(file, editor.getDocument(), editor.getCaretModel().getOffset());
    if (offset == 0) {
      return null;
    }
    PsiElement atCaret = file.findElementAt(offset);

    SliceLanguageSupportProvider provider = LanguageSlicing.getProvider(file);
    if (provider == null || atCaret == null) {
      return null;
    }
    return provider.getExpressionAtCaret(atCaret, myDataFlowToThis);
  }

  public abstract SliceAnalysisParams askForParams(PsiElement element,
                                                   SliceManager.StoredSettingsBean storedSettingsBean,
                                                   String dialogTitle);

  public static SliceHandler create(boolean dataFlowToThis) {
    return dataFlowToThis ? new SliceBackwardHandler() : new SliceForwardHandler();
  }
}
