// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.codeInspection.ex;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.codeInspection.BatchQuickFix;
import com.intellij.codeInspection.CommonProblemDescriptor;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.QuickFix;
import com.intellij.codeInspection.reference.RefElement;
import com.intellij.codeInspection.reference.RefEntity;
import com.intellij.codeInspection.reference.RefManager;
import com.intellij.codeInspection.ui.InspectionToolPresentation;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * @author max
 */
public class LocalQuickFixWrapper extends QuickFixAction {
  private final QuickFix myFix;

  public LocalQuickFixWrapper(@NotNull QuickFix fix, @NotNull InspectionToolWrapper toolWrapper) {
    super(fix.getName(), toolWrapper);
    myFix = fix;
    setText(StringUtil.escapeMnemonics(myFix.getName()));
  }

  public void setText(@NotNull String text) {
    getTemplatePresentation().setText(text);
  }

  @Override
  protected boolean isProblemDescriptorsAcceptable() {
    return true;
  }

  @NotNull
  public QuickFix getFix() {
    return myFix;
  }

  @Nullable
  private QuickFix getWorkingQuickFix(@NotNull QuickFix[] fixes) {
    for (QuickFix fix : fixes) {
      if (fix.getFamilyName().equals(myFix.getFamilyName())) {
        return fix;
      }
    }
    return null;
  }

  @Override
  protected boolean applyFix(@NotNull RefEntity[] refElements) {
    return true;
  }

  @Override
  protected void applyFix(@NotNull final Project project,
                          @NotNull final GlobalInspectionContextImpl context,
                          @NotNull final CommonProblemDescriptor[] descriptors,
                          @NotNull final Set<? super PsiElement> ignoredElements) {
    if (myFix instanceof BatchQuickFix) {
      final List<PsiElement> collectedElementsToIgnore = new ArrayList<>();
      final Runnable refreshViews = () -> {
        DaemonCodeAnalyzer.getInstance(project).restart();
        for (CommonProblemDescriptor descriptor : descriptors) {
          ignore(ignoredElements, descriptor, getWorkingQuickFix(descriptor.getFixes()) != null, context);
        }

        final RefManager refManager = context.getRefManager();
        final RefElement[] refElements = new RefElement[collectedElementsToIgnore.size()];
        for (int i = 0, collectedElementsToIgnoreSize = collectedElementsToIgnore.size(); i < collectedElementsToIgnoreSize; i++) {
          refElements[i] = refManager.getReference(collectedElementsToIgnore.get(i));
        }

        removeElements(refElements, project, myToolWrapper);
      };

      ((BatchQuickFix)myFix).applyFix(project, descriptors, collectedElementsToIgnore, refreshViews);
      return;
    }

    boolean restart = false;
    for (CommonProblemDescriptor descriptor : descriptors) {
      if (descriptor == null) continue;
      final QuickFix[] fixes = descriptor.getFixes();
      if (fixes != null) {
        final QuickFix fix = getWorkingQuickFix(fixes);
        if (fix != null) {
          //CCE here means QuickFix was incorrectly inherited, is there a way to signal (plugin) it is wrong?
          fix.applyFix(project, descriptor);
          restart = true;
          ignore(ignoredElements, descriptor, true, context);
        }
      }
    }
    if (restart) {
      DaemonCodeAnalyzer.getInstance(project).restart();
    }
  }

  @Override
  protected boolean startInWriteAction() {
    return myFix.startInWriteAction();
  }

  @Override
  protected void performFixesInBatch(@NotNull Project project,
                                     @NotNull List<CommonProblemDescriptor[]> descriptors,
                                     @NotNull GlobalInspectionContextImpl context,
                                     Set<? super PsiElement> ignoredElements) {
    if (myFix instanceof BatchQuickFix) {
      applyFix(project, context, BatchModeDescriptorsUtil.flattenDescriptors(descriptors), ignoredElements);
    }
    else {
      super.performFixesInBatch(project, descriptors, context, ignoredElements);
    }
  }

  private void ignore(@NotNull Collection<? super PsiElement> ignoredElements,
                      @NotNull CommonProblemDescriptor descriptor,
                      boolean hasFix,
                      @NotNull GlobalInspectionContextImpl context) {
    if (hasFix) {
      InspectionToolPresentation presentation = context.getPresentation(myToolWrapper);
      presentation.resolveProblem(descriptor);
    }
    if (descriptor instanceof ProblemDescriptor) {
      PsiElement element = ((ProblemDescriptor)descriptor).getPsiElement();
      if (element != null) {
        ignoredElements.add(element);
      }
    }
  }
}