// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.analysis.problemsView;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.xml.util.XmlStringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface AnalysisProblem {
  @NotNull String getErrorMessage();

  @Nullable String getCorrectionMessage();

  @Nullable String getUrl();

  @NotNull String getCode();

  String getSeverity();

  int getLineNumber();

  int getOffset();

  @NotNull String getSystemIndependentPath();

  @NotNull String getPresentableLocationWithoutLineNumber();

  @NotNull String getPresentableLocation();

  @Nullable VirtualFile getFile();

  @Nullable VirtualFile getPackageRoot();

  @Nullable VirtualFile getContentRoot();

  @NotNull
  List<AnalysisProblem> getSecondaryMessages();

  @NotNull String getTooltip();

  @NotNull
  static String generateTooltipText(@NotNull String message, @Nullable String correction, @Nullable String url) {
    final StringBuilder tooltip = new StringBuilder("<html>").append(XmlStringUtil.escapeString(message));
    if (StringUtil.isNotEmpty(correction)) {
      tooltip.append("<br/><br/>").append(XmlStringUtil.escapeString(correction));
    }
    if (StringUtil.isNotEmpty(url)) {
      tooltip.append("<br/><a href='").append(url).append("'>Open documentation</a>");
    }
    tooltip.append("</html>");
    return tooltip.toString();
  }
}
