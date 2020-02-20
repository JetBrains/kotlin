// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.analysis.problemsView;

import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public abstract class AnalysisProblemsPresentationHelper {
  @NotNull
  RowFilter<AnalysisProblemsTableModel, Integer> getRowFilter() {
    return new RowFilter<AnalysisProblemsTableModel, Integer>() {
      @Override
      public boolean include(@NotNull final Entry<? extends AnalysisProblemsTableModel, ? extends Integer> entry) {
        return shouldShowProblem(entry.getModel().getItem(entry.getIdentifier()));
      }
    };
  }

  public abstract void resetAllFilters();

  public abstract boolean areFiltersApplied();

  public abstract boolean isAutoScrollToSource();

  public abstract void setAutoScrollToSource(boolean autoScroll);

  public abstract boolean isGroupBySeverity();

  public abstract void setGroupBySeverity(boolean groupBySeverity);

  public abstract boolean isShowErrors();

  public abstract boolean isShowWarnings();

  public abstract boolean isShowHints();

  @Nullable
  public abstract VirtualFile getCurrentFile();

  public abstract boolean shouldShowProblem(@NotNull AnalysisProblem problem);

  @NotNull
  public abstract String getFilterTypeText();
}
