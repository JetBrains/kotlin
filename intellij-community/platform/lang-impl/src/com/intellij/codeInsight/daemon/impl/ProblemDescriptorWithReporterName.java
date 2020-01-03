// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.daemon.impl;

import com.intellij.codeInspection.ProblemDescriptorBase;
import org.jetbrains.annotations.NotNull;

public class ProblemDescriptorWithReporterName extends ProblemDescriptorBase {
  private final String myReportingToolName;

  public ProblemDescriptorWithReporterName(@NotNull ProblemDescriptorBase pd,
                                           @NotNull String reportingToolName) {
    super(pd.getStartElement(), pd.getEndElement(), pd.getDescriptionTemplate(), pd.getFixes(),
          pd.getHighlightType(), pd.isAfterEndOfLine(), pd.getTextRangeInElement(), pd.showTooltip(), pd.isOnTheFly());
    myReportingToolName = reportingToolName;
  }

  @NotNull
  public String getReportingToolName() {
    return myReportingToolName;
  }
}
