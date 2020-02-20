// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.analysis.problemsView;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.ListTableModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

public class AnalysisProblemsTableModel extends ListTableModel<AnalysisProblem> {
  private static final TableCellRenderer MESSAGE_RENDERER = new DefaultTableCellRenderer() {
    @Override
    public JLabel getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
      // Do not emphasize a focused cell, drawing the whole row as selected is enough
      final JLabel label = (JLabel)super.getTableCellRendererComponent(table, value, isSelected, false, row, column);

      final AnalysisProblem problem = (AnalysisProblem)value;
      setText(problem.getErrorMessage().replaceAll("(\n)+", " "));

      setToolTipText(problem.getTooltip());

      final String severity = problem.getSeverity();
      setIcon(AnalysisErrorSeverity.ERROR.equals(severity)
              ? AllIcons.General.Error
              : AnalysisErrorSeverity.WARNING.equals(severity)
                ? AllIcons.General.Warning
                : AllIcons.General.Information);

      return label;
    }
  };

  private static final TableCellRenderer LOCATION_RENDERER = new DefaultTableCellRenderer() {
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
      // Do not emphasize a focused cell, drawing the whole row as selected is enough
      return super.getTableCellRendererComponent(table, value, isSelected, false, row, column);
    }
  };

  @NotNull private final AnalysisProblemsPresentationHelper myPresentationHelper;

  // Kind of hack to keep a reference to the live collection used in a super class, but it allows improving performance greatly.
  // Having it in hand we can do bulk rows removal with a single fireTableRowsDeleted() call afterwards
  private final List<AnalysisProblem> myItems = new ArrayList<>();

  private RowSorter.SortKey mySortKey = new RowSorter.SortKey(1, SortOrder.ASCENDING);

  protected int myErrorCount;
  protected int myWarningCount;
  protected int myHintCount;

  private int myErrorCountAfterFilter;
  private int myWarningCountAfterFilter;
  private int myHintCountAfterFilter;

  private final Comparator<AnalysisProblem> myDescriptionComparator = new ProblemComparator(ProblemComparator.MESSAGE_COLUMN_ID);
  private final Comparator<AnalysisProblem> myLocationComparator = new ProblemComparator(ProblemComparator.LOCATION_COLUMN_ID);

  public AnalysisProblemsTableModel(@NotNull AnalysisProblemsPresentationHelper presentationHelper) {
    myPresentationHelper = presentationHelper;
    setColumnInfos(new ColumnInfo[]{createDescriptionColumn(), createLocationColumn()});
    setItems(myItems);
    setSortable(true);
  }

  @NotNull
  private ColumnInfo<AnalysisProblem, AnalysisProblem> createDescriptionColumn() {
    return new ColumnInfo<AnalysisProblem, AnalysisProblem>("Description") {
      @Nullable
      @Override
      public Comparator<AnalysisProblem> getComparator() {
        return myDescriptionComparator;
      }

      @Nullable
      @Override
      public TableCellRenderer getRenderer(@NotNull final AnalysisProblem problem) {
        return MESSAGE_RENDERER;
      }

      @NotNull
      @Override
      public AnalysisProblem valueOf(@NotNull final AnalysisProblem problem) {
        return problem;
      }
    };
  }

  @NotNull
  private ColumnInfo<AnalysisProblem, String> createLocationColumn() {
    return new ColumnInfo<AnalysisProblem, String>("Location") {
      @Nullable
      @Override
      public Comparator<AnalysisProblem> getComparator() {
        return myLocationComparator;
      }

      @Nullable
      @Override
      public TableCellRenderer getRenderer(AnalysisProblem problem) {
        return LOCATION_RENDERER;
      }

      @NotNull
      @Override
      public String valueOf(@NotNull final AnalysisProblem problem) {
        return problem.getPresentableLocation();
      }
    };
  }

  @Override
  public RowSorter.SortKey getDefaultSortKey() {
    return mySortKey;
  }

  @Override
  public boolean canExchangeRows(int oldIndex, int newIndex) {
    return false;
  }

  @Override
  public void exchangeRows(int idx1, int idx2) {
    throw new IllegalStateException();
  }

  @Override
  public boolean isCellEditable(int rowIndex, int columnIndex) {
    return false;
  }

  protected void removeRows(final int firstRow, final int lastRow) {
    assert lastRow >= firstRow;

    for (int i = lastRow; i >= firstRow; i--) {
      final AnalysisProblem removed = myItems.remove(i);

      if (AnalysisErrorSeverity.ERROR.equals(removed.getSeverity())) myErrorCount--;
      if (AnalysisErrorSeverity.WARNING.equals(removed.getSeverity())) myWarningCount--;
      if (AnalysisErrorSeverity.INFO.equals(removed.getSeverity())) myHintCount--;
      updateProblemsCountAfterFilter(removed, -1);
    }

    fireTableRowsDeleted(firstRow, lastRow);
  }

  public void removeRows(@NotNull Predicate<? super AnalysisProblem> predicate) {
    int firstRow = getRowCount();
    int lastRow = -1;
    for (int i = getRowCount()-1; i >= 0; i--) {
      AnalysisProblem problem = myItems.get(i);
      if (!predicate.test(problem)) continue;
      myItems.remove(i);
      if (AnalysisErrorSeverity.ERROR.equals(problem.getSeverity())) myErrorCount--;
      if (AnalysisErrorSeverity.WARNING.equals(problem.getSeverity())) myWarningCount--;
      if (AnalysisErrorSeverity.INFO.equals(problem.getSeverity())) myHintCount--;
      updateProblemsCountAfterFilter(problem, -1);
      firstRow = Math.min(firstRow, i);
      lastRow = Math.max(lastRow, i);
    }

    if (firstRow <= lastRow) {
      fireTableRowsDeleted(firstRow, lastRow);
    }
  }

  public void removeAll() {
    final int rowCount = getRowCount();
    if (rowCount > 0) {
      myItems.clear();
      fireTableRowsDeleted(0, rowCount - 1);
    }

    myErrorCount = 0;
    myWarningCount = 0;
    myHintCount = 0;
    myErrorCountAfterFilter = 0;
    myWarningCountAfterFilter = 0;
    myHintCountAfterFilter = 0;
  }

  @Nullable
  public AnalysisProblem addProblemsAndReturnReplacementForSelection(@NotNull List<? extends AnalysisProblem> problems,
                                                                     @Nullable final AnalysisProblem oldSelectedProblem) {
    AnalysisProblem newSelectedProblem = null;
    if (!problems.isEmpty()) {
      for (AnalysisProblem problem : problems) {
        if (oldSelectedProblem != null &&
            lookSimilar(problem, oldSelectedProblem) &&
            (newSelectedProblem == null ||
             // check if current problem is closer to oldSelectedProblem
             (Math.abs(oldSelectedProblem.getLineNumber() - newSelectedProblem.getLineNumber()) >=
              Math.abs(oldSelectedProblem.getLineNumber() - problem.getLineNumber())))) {
          newSelectedProblem = problem;
        }

        if (AnalysisErrorSeverity.ERROR.equals(problem.getSeverity())) myErrorCount++;
        if (AnalysisErrorSeverity.WARNING.equals(problem.getSeverity())) myWarningCount++;
        if (AnalysisErrorSeverity.INFO.equals(problem.getSeverity())) myHintCount++;
        updateProblemsCountAfterFilter(problem, +1);
      }
      addRows(problems);
    }

    return newSelectedProblem;
  }


  protected static boolean lookSimilar(@NotNull AnalysisProblem problem1, @NotNull AnalysisProblem problem2) {
    return problem1.getSeverity().equals(problem2.getSeverity()) &&
           problem1.getErrorMessage().equals(problem2.getErrorMessage()) &&
           problem1.getSystemIndependentPath().equals(problem2.getSystemIndependentPath());
  }

  protected void updateProblemsCountAfterFilter(@NotNull final AnalysisProblem problem, int delta) {
    if (myPresentationHelper.shouldShowProblem(problem)) {
      if (AnalysisErrorSeverity.ERROR.equals(problem.getSeverity())) myErrorCountAfterFilter+=delta;
      if (AnalysisErrorSeverity.WARNING.equals(problem.getSeverity())) myWarningCountAfterFilter+=delta;
      if (AnalysisErrorSeverity.INFO.equals(problem.getSeverity())) myHintCountAfterFilter+=delta;
    }
  }

  void setSortKey(@NotNull final RowSorter.SortKey sortKey) {
    mySortKey = sortKey;
  }

  void onFilterChanged() {
    ApplicationManager.getApplication().assertIsDispatchThread();
    if (myPresentationHelper.areFiltersApplied()) {
      myErrorCountAfterFilter = 0;
      myWarningCountAfterFilter = 0;
      myHintCountAfterFilter = 0;
      for (AnalysisProblem problem : myItems) {
        updateProblemsCountAfterFilter(problem, +1);
      }
    }
    else {
      myErrorCountAfterFilter = myErrorCount;
      myWarningCountAfterFilter = myWarningCount;
      myHintCountAfterFilter = myHintCount;
    }
  }

  public boolean hasErrors() {
    return myErrorCount > 0;
  }

  public boolean hasWarnings() {
    return myWarningCount > 0;
  }

  @NotNull
  public String getStatusText() {
    final List<String> summary = new ArrayList<>();

    if (myPresentationHelper.isShowErrors() && myErrorCountAfterFilter > 0) {
      summary.add(myErrorCountAfterFilter + " " + StringUtil.pluralize("error", myErrorCountAfterFilter));
    }
    if (myPresentationHelper.isShowWarnings() && myWarningCountAfterFilter > 0) {
      summary.add(myWarningCountAfterFilter + " " + StringUtil.pluralize("warning", myWarningCountAfterFilter));
    }
    if (myPresentationHelper.isShowHints() && myHintCountAfterFilter > 0) {
      summary.add(myHintCountAfterFilter + " " + StringUtil.pluralize("hint", myHintCountAfterFilter));
    }


    if (summary.isEmpty()) {
      return myPresentationHelper.areFiltersApplied() ? myPresentationHelper.getFilterTypeText() : "";
    }

    final StringBuilder b = new StringBuilder();
    if (summary.size() == 2) {
      b.append(StringUtil.join(summary, " and "));
    }
    else {
      b.append(StringUtil.join(summary, ", "));
    }

    if (myPresentationHelper.areFiltersApplied()) {
      b.append(" (");
      b.append(myPresentationHelper.getFilterTypeText());
      b.append(")");
    }

    return b.toString();
  }

  private class ProblemComparator implements Comparator<AnalysisProblem> {
    private static final int MESSAGE_COLUMN_ID = 0;
    private static final int LOCATION_COLUMN_ID = 1;

    private final int myColumn;

    ProblemComparator(final int column) {
      myColumn = column;
    }

    @Override
    public int compare(@NotNull final AnalysisProblem problem1, @NotNull final AnalysisProblem problem2) {
      if (myPresentationHelper.isGroupBySeverity()) {
        final int s1 = getSeverityIndex(problem1);
        final int s2 = getSeverityIndex(problem2);
        if (s1 != s2) {
          // Regardless of sorting direction, if 'Group by severity' is selected then we should keep errors on top
          return mySortKey.getSortOrder() == SortOrder.ASCENDING ? s1 - s2 : s2 - s1;
        }
      }

      if (myColumn == MESSAGE_COLUMN_ID) {
        return StringUtil.compare(problem1.getErrorMessage(), problem2.getErrorMessage(), false);
      }

      if (myColumn == LOCATION_COLUMN_ID) {
        final int result = StringUtil.compare(problem1.getPresentableLocationWithoutLineNumber(),
                                              problem2.getPresentableLocationWithoutLineNumber(), false);
        if (result != 0) {
          return result;
        }
        else {
          // Regardless of sorting direction, line numbers within the same file should be sorted in ascending order
          return mySortKey.getSortOrder() == SortOrder.ASCENDING
                 ? problem1.getLineNumber() - problem2.getLineNumber()
                 : problem2.getLineNumber() - problem1.getLineNumber();
        }
      }

      return 0;
    }

    private int getSeverityIndex(@NotNull final AnalysisProblem problem) {
      final String severity = problem.getSeverity();
      if (AnalysisErrorSeverity.ERROR.equals(severity)) {
        return 0;
      }
      if (AnalysisErrorSeverity.WARNING.equals(severity)) {
        return 1;
      }
      return 2;
    }
  }
}
