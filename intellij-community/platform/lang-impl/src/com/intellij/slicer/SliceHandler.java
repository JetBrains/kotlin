/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.intellij.slicer;

import com.intellij.analysis.AnalysisScope;
import com.intellij.analysis.AnalysisUIOptions;
import com.intellij.analysis.BaseAnalysisActionDialog;
import com.intellij.analysis.dialog.ModelScopeItem;
import com.intellij.codeInsight.CodeInsightActionHandler;
import com.intellij.codeInsight.TargetElementUtil;
import com.intellij.codeInsight.hint.HintManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @author cdr
 */
public class SliceHandler implements CodeInsightActionHandler {
  private static final Logger LOG = Logger.getInstance(SliceHandler.class);
  private final boolean myDataFlowToThis;

  public SliceHandler(boolean dataFlowToThis) {
    myDataFlowToThis = dataFlowToThis;
  }

  @Override
  public void invoke(@NotNull final Project project, @NotNull final Editor editor, @NotNull final PsiFile file) {
    PsiElement expression = getExpressionAtCaret(editor, file);
    if (expression == null) {
      HintManager.getInstance().showErrorHint(editor, "Cannot find what to analyze. Please stand on the expression or variable or method parameter and try again.");
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
    if(provider == null || atCaret == null) {
      return null;
    }
    return provider.getExpressionAtCaret(atCaret, myDataFlowToThis);
  }

  public SliceAnalysisParams askForParams(PsiElement element, boolean dataFlowToThis, SliceManager.StoredSettingsBean storedSettingsBean, String dialogTitle) {
    AnalysisScope analysisScope = new AnalysisScope(element.getContainingFile());
    Module module = ModuleUtilCore.findModuleForPsiElement(element);

    Project myProject = element.getProject();
    AnalysisUIOptions analysisUIOptions = new AnalysisUIOptions();
    analysisUIOptions.loadState(storedSettingsBean.analysisUIOptions);

    List<ModelScopeItem> items = BaseAnalysisActionDialog.standardItems(myProject, analysisScope,
                                                                        module, element);
    BaseAnalysisActionDialog dialog =
      new BaseAnalysisActionDialog(dialogTitle, "Analyze scope", myProject, items, analysisUIOptions, true);
    if (!dialog.showAndGet()) {
      return null;
    }

    AnalysisScope scope = dialog.getScope(analysisUIOptions, analysisScope, myProject, module);
    storedSettingsBean.analysisUIOptions.loadState(analysisUIOptions);

    SliceAnalysisParams params = new SliceAnalysisParams();
    params.scope = scope;
    params.dataFlowToThis = dataFlowToThis;
    return params;
  }
}
