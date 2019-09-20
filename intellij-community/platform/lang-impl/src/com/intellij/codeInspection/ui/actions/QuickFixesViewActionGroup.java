/*
 * Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */
package com.intellij.codeInspection.ui.actions;

import com.intellij.codeInspection.ex.InspectionToolWrapper;
import com.intellij.codeInspection.ex.QuickFixAction;
import com.intellij.codeInspection.ui.InspectionResultsView;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.intellij.codeInspection.ui.actions.InspectionViewActionBase.getView;

/**
 * @author Dmitry Batkovich
 */
public class QuickFixesViewActionGroup extends ActionGroup {
  @NotNull
  @Override
  public AnAction[] getChildren(@Nullable AnActionEvent e) {
    InspectionResultsView view = getView(e);
    if (view == null || InvokeQuickFixAction.cantApplyFixes(view)) return AnAction.EMPTY_ARRAY;
    InspectionToolWrapper toolWrapper = view.getTree().getSelectedToolWrapper(true);
    if (toolWrapper == null) return AnAction.EMPTY_ARRAY;
    QuickFixAction[] quickFixes = view.getProvider().getCommonQuickFixes(toolWrapper, view.getTree());
    if (quickFixes.length != 0) return quickFixes;
    return view.getProvider().getPartialQuickFixes(toolWrapper, view.getTree());
  }
}
