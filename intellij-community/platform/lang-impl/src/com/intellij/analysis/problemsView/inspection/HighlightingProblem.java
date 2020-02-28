// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.analysis.problemsView.inspection;

import com.intellij.analysis.problemsView.AnalysisErrorSeverity;
import com.intellij.analysis.problemsView.AnalysisProblem;
import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.RangeMarker;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.ObjectUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * Problem from the "Problems View" toolwindow based on HighlightInfo, created by GeneralHighlightingPass/LocalInspectionPass
 */
class HighlightingProblem implements AnalysisProblem {
  @NotNull
  private final Project myProject;
  @NotNull
  private final VirtualFile myVirtualFile;
  @NotNull
  private final HighlightInfo myInfo;

  HighlightingProblem(@NotNull Project project, @NotNull VirtualFile virtualFile, @NotNull HighlightInfo highlightInfo) {
    myProject = project;
    myVirtualFile = virtualFile;
    myInfo = highlightInfo;
  }

  @Override
  public @NotNull String getErrorMessage() {
    return myInfo.getDescription();
  }

  @Override
  public @Nullable String getCorrectionMessage() {
    return null;
  }

  @Override
  public @Nullable String getUrl() {
    return null; // avoid useless "show documentation"
  }

  @Override
  public @NotNull String getCode() {
    return "";
  }

  @Override
  public String getSeverity() {
    if (myInfo.getSeverity() == HighlightSeverity.ERROR) return AnalysisErrorSeverity.ERROR;
    if (myInfo.getSeverity() == HighlightSeverity.WARNING) return AnalysisErrorSeverity.WARNING;
    return AnalysisErrorSeverity.INFO;
  }

  @Override
  public int getLineNumber() {
    PsiFile psiFile = PsiManager.getInstance(myProject).findFile(myVirtualFile);
    Document document = psiFile == null ? null : PsiDocumentManager.getInstance(myProject).getDocument(psiFile);
    int offset = document == null ? -1 : myInfo.getActualStartOffset();
    return 0 <= offset && offset < document.getTextLength() ? document.getLineNumber(offset) + 1 : 0;
  }

  @Override
  public int getOffset() {
    return myInfo.getActualStartOffset();
  }

  @Override
  public @NotNull String getSystemIndependentPath() {
    return getFile().getPath();
  }

  @Override
  public @NotNull String getPresentableLocationWithoutLineNumber() {
    return myVirtualFile.getName();
  }

  @Override
  public @NotNull String getPresentableLocation() {
    return getPresentableLocationWithoutLineNumber()+":"+getLineNumber();
  }

  @Override
  public @NotNull VirtualFile getFile() {
    return myVirtualFile;
  }

  @Override
  public @Nullable VirtualFile getPackageRoot() {
    return null;
  }

  @Override
  public @Nullable VirtualFile getContentRoot() {
    return null;
  }

  @Override
  public @NotNull List<AnalysisProblem> getSecondaryMessages() {
    return Collections.emptyList();
  }

  @Override
  public @NotNull String getTooltip() {
    return ObjectUtils.notNull(myInfo.getToolTip(), getErrorMessage());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    HighlightingProblem problem = (HighlightingProblem)o;

    return myInfo.equals(problem.myInfo);
  }

  @Override
  public int hashCode() {
    return myInfo.hashCode();
  }

  @Override
  public String toString() {
    return "H: "+getPresentableLocation();
  }

  @NotNull
  List<Pair<HighlightInfo.IntentionActionDescriptor, RangeMarker>> getFixes() {
    return ObjectUtils.notNull(myInfo.quickFixActionMarkers, Collections.emptyList());
  }
}
