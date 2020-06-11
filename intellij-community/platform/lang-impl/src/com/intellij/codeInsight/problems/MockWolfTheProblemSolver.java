// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.codeInsight.problems;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.problems.Problem;
import com.intellij.problems.WolfTheProblemSolver;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

public class MockWolfTheProblemSolver extends WolfTheProblemSolver {
  private WolfTheProblemSolver myDelegate;

  @Override
  public boolean isProblemFile(@NotNull VirtualFile virtualFile) {
    return myDelegate != null && myDelegate.isProblemFile(virtualFile);
  }

  @Override
  public void weHaveGotProblems(@NotNull final VirtualFile virtualFile, @NotNull final List<? extends Problem> problems) {
    if (myDelegate != null) myDelegate.weHaveGotProblems(virtualFile, problems);
  }

  @Override
  public void weHaveGotNonIgnorableProblems(@NotNull VirtualFile virtualFile, @NotNull List<? extends Problem> problems) {
    if (myDelegate != null) myDelegate.weHaveGotNonIgnorableProblems(virtualFile, problems);
  }

  @Override
  public boolean hasProblemFilesBeneath(@NotNull final Condition<? super VirtualFile> condition) {
    return false;
  }

  @Override
  public boolean hasSyntaxErrors(final @NotNull VirtualFile file) {
    return myDelegate != null && myDelegate.hasSyntaxErrors(file);
  }

  @Override
  public boolean hasProblemFilesBeneath(@NotNull Module scope) {
    return myDelegate != null && myDelegate.hasProblemFilesBeneath(scope);
  }

  @Override
  public void addProblemListener(@NotNull WolfTheProblemSolver.ProblemListener listener, @NotNull Disposable parentDisposable) {
    if (myDelegate != null) myDelegate.addProblemListener(listener, parentDisposable);
  }

  @Override
  public void queue(@NotNull VirtualFile suspiciousFile) {
    if (myDelegate != null) myDelegate.queue(suspiciousFile);
  }

  @Override
  public void clearProblems(@NotNull VirtualFile virtualFile) {
    if (myDelegate != null) myDelegate.clearProblems(virtualFile);
  }

  @Override
  public Problem convertToProblem(@NotNull VirtualFile virtualFile, int line, int column, String @NotNull [] message) {
    return myDelegate == null ? null : myDelegate.convertToProblem(virtualFile, line, column, message);
  }

  void setDelegate(@NotNull WolfTheProblemSolver delegate) {
    if (myDelegate != null) {
      throw new IllegalStateException();
    }
    myDelegate = delegate;
  }

  @Override
  public void reportProblems(final @NotNull VirtualFile file, @NotNull Collection<? extends Problem> problems) {
    if (myDelegate != null) myDelegate.reportProblems(file,problems);
  }

  @Override
  public void reportProblemsFromExternalSource(@NotNull VirtualFile file, @NotNull Object source) {
    if (myDelegate != null) myDelegate.reportProblemsFromExternalSource(file, source);
  }

  @Override
  public void clearProblemsFromExternalSource(@NotNull VirtualFile file, @NotNull Object source) {
    if (myDelegate != null) myDelegate.clearProblemsFromExternalSource(file, source);
  }
}
