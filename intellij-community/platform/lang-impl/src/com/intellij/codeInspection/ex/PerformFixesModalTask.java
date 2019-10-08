// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInspection.ex;

import com.intellij.codeInspection.CommonProblemDescriptor;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.QuickFix;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.PostprocessReformattingAspect;
import com.intellij.psi.presentation.java.SymbolPresentationUtil;
import com.intellij.util.SequentialTask;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public abstract class PerformFixesModalTask implements SequentialTask {
  @NotNull
  protected final Project myProject;
  private final List<CommonProblemDescriptor[]> myDescriptorPacks;
  private final PsiDocumentManager myDocumentManager;
  private final PostprocessReformattingAspect myReformattingAspect;
  private final int myLength;

  private int myProcessed = 0;
  private int myPackIdx = 0;
  private int myDescriptorIdx = 0;

  protected PerformFixesModalTask(@NotNull Project project,
                                  @NotNull CommonProblemDescriptor[] descriptors) {
    this(project, Collections.singletonList(descriptors));
  }

  protected PerformFixesModalTask(@NotNull Project project,
                                  @NotNull List<CommonProblemDescriptor[]> descriptorPacks) {
    myProject = project;
    myDescriptorPacks = descriptorPacks;
    myLength = descriptorPacks.stream().mapToInt(ds -> ds.length).sum();
    myDocumentManager = PsiDocumentManager.getInstance(myProject);
    myReformattingAspect = PostprocessReformattingAspect.getInstance(myProject);
  }

  @Override
  public boolean isDone() {
    return myPackIdx > myDescriptorPacks.size() - 1;
  }

  @Override
  public boolean iteration() {
    return true;
  }

  public void doRun(ProgressIndicator indicator) {
    indicator.setIndeterminate(false);
    while (!isDone()) {
      if (indicator.isCanceled()) {
        break;
      }
      iteration(indicator);
    }
  }

  @Override
  public boolean iteration(@NotNull ProgressIndicator indicator) {
    final Pair<CommonProblemDescriptor, Boolean> pair = nextDescriptor();
    CommonProblemDescriptor descriptor = pair.getFirst();
    boolean shouldDoPostponedOperations = pair.getSecond();

    indicator.setFraction((double)myProcessed++ / myLength);
    String presentableText = "usages";
    if (descriptor instanceof ProblemDescriptor) {
      final PsiElement psiElement = ((ProblemDescriptor)descriptor).getPsiElement();
      if (psiElement != null) {
        presentableText = SymbolPresentationUtil.getSymbolPresentableText(psiElement);
      }
    }
    indicator.setText("Processing " + presentableText);

    final boolean[] runInReadAction = {false};
    final QuickFix[] fixes = descriptor.getFixes();
    if (fixes != null) {
      for (QuickFix fix : fixes) {
        if (!fix.startInWriteAction()) {
          runInReadAction[0] = true;
        } else {
          runInReadAction[0] = false;
          break;
        }
      }
    }

    ApplicationManager.getApplication().runWriteAction(() -> {
      myDocumentManager.commitAllDocuments();
      if (!runInReadAction[0]) {
        applyFix(myProject, descriptor);
        if (shouldDoPostponedOperations) {
          myReformattingAspect.doPostponedFormatting();
        }
      }
    });
    if (runInReadAction[0]) {
      applyFix(myProject, descriptor);
    }
    return isDone();
  }

  protected abstract void applyFix(Project project, CommonProblemDescriptor descriptor);

  private Pair<CommonProblemDescriptor, Boolean> nextDescriptor() {
    CommonProblemDescriptor[] descriptors = myDescriptorPacks.get(myPackIdx);
    CommonProblemDescriptor descriptor = descriptors[myDescriptorIdx++];
    boolean shouldDoPostponedOperations = false;
    if (myDescriptorIdx == descriptors.length) {
      shouldDoPostponedOperations = true;
      myPackIdx++;
      myDescriptorIdx = 0;
    }
    return Pair.create(descriptor, shouldDoPostponedOperations);
  }
}